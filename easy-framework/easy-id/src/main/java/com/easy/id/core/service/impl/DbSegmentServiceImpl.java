package com.easy.id.core.service.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.easy.id.core.model.Segment;
import com.easy.id.core.service.SegmentService;
import com.easy.id.exception.IdGeneratorException;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于数据库的号段服务实现
 */
@Slf4j
public class DbSegmentServiceImpl implements SegmentService {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    /**
     * SQL语句
     */
    private String updateSql;
    private String selectSql;
    private String insertSql;

    /**
     * 构造方法
     *
     * @param jdbcTemplate JDBC模板
     * @param tableName    表名
     */
    public DbSegmentServiceImpl(JdbcTemplate jdbcTemplate, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        initSqlTemplate();
    }

    /**
     * 初始化SQL模板
     */
    private void initSqlTemplate() {
        updateSql = "UPDATE " + tableName + " SET current_value = current_value + step, version = version + 1, " +
                "update_time = NOW() WHERE business_key = ? AND version = ?";

        selectSql = "SELECT id, business_key, current_value, step, version FROM " + tableName +
                " WHERE business_key = ?";

        insertSql = "INSERT INTO " + tableName +
                "(business_key, current_value, step, version, create_time, update_time) " +
                "VALUES (?, ?, ?, 0, NOW(), NOW())";
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Segment getNextSegment(String businessKey, int step) {
        try {
            return doGetNextSegment(businessKey, step);
        } catch (Exception e) {
            log.error("获取号段失败", e);
            throw new IdGeneratorException("获取号段失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取下一个号段的具体实现
     */
    private Segment doGetNextSegment(String businessKey, int step) {
        // 1. 查询当前业务键的记录
        List<BusinessRecord> records = jdbcTemplate.query(selectSql, new BusinessRecordRowMapper(), businessKey);

        // 2. 如果不存在，则初始化
        if (records.isEmpty()) {
            initBusinessKey(businessKey, step);
            records = jdbcTemplate.query(selectSql, new BusinessRecordRowMapper(), businessKey);
            if (records.isEmpty()) {
                throw new IdGeneratorException("初始化业务键失败: " + businessKey);
            }
        }

        BusinessRecord record = records.get(0);

        // 3. 更新号段值（乐观锁）
        int updated = jdbcTemplate.update(updateSql, businessKey, record.getVersion());
        if (updated == 0) {
            // 更新失败，说明已被其他线程更新，重试
            return getNextSegment(businessKey, step);
        }

        // 4. 创建号段对象
        Segment segment = new Segment();
        segment.setBusinessKey(businessKey);
        segment.setStep(step);

        // 当前值 = 更新前的current_value
        segment.setCurrentValue(new AtomicLong(record.getCurrentValue()));

        // 最大值 = 当前值 + 步长
        segment.setMaxValue(record.getCurrentValue() + step);

        log.info("获取号段成功: {}, 范围: [{}, {}]", businessKey,
                segment.getCurrentValue().get(), segment.getMaxValue());

        return segment;
    }

    @Override
    public void initBusinessKey(String businessKey, int step) {
        try {
            List<BusinessRecord> records = jdbcTemplate.query(selectSql, new BusinessRecordRowMapper(), businessKey);

            if (records.isEmpty()) {
                log.info("初始化业务键: {}, 步长: {}", businessKey, step);
                int inserted = jdbcTemplate.update(insertSql, businessKey, 0, step);
                if (inserted == 0) {
                    throw new IdGeneratorException("初始化业务键失败: " + businessKey);
                }
            } else {
                log.info("业务键已存在: {}", businessKey);
            }
        } catch (DataAccessException e) {
            log.error("初始化业务键失败", e);
            throw new IdGeneratorException("初始化业务键失败: " + e.getMessage(), e);
        }
    }

    /**
     * 业务记录模型
     */
    private static class BusinessRecord {
        private long id;
        private String businessKey;
        private long currentValue;
        private int step;
        private int version;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getBusinessKey() {
            return businessKey;
        }

        public void setBusinessKey(String businessKey) {
            this.businessKey = businessKey;
        }

        public long getCurrentValue() {
            return currentValue;
        }

        public void setCurrentValue(long currentValue) {
            this.currentValue = currentValue;
        }

        public int getStep() {
            return step;
        }

        public void setStep(int step) {
            this.step = step;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }

    /**
     * 业务记录行映射器
     */
    private static class BusinessRecordRowMapper implements RowMapper<BusinessRecord> {
        @Override
        public BusinessRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            BusinessRecord record = new BusinessRecord();
            record.setId(rs.getLong("id"));
            record.setBusinessKey(rs.getString("business_key"));
            record.setCurrentValue(rs.getLong("current_value"));
            record.setStep(rs.getInt("step"));
            record.setVersion(rs.getInt("version"));
            return record;
        }
    }
}