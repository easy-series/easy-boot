package com.easy.id.core.service.impl;

import com.easy.id.core.model.Segment;
import com.easy.id.core.service.SegmentService;
import com.easy.id.exception.IdGeneratorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

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
    private String updateNextValueSql;
    private String selectByBusinessKeySql;
    private String insertBusinessKeySql;
    
    public DbSegmentServiceImpl(JdbcTemplate jdbcTemplate, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        initSqlTemplate();
    }

    private void initSqlTemplate() {
        updateNextValueSql = "UPDATE " + tableName + " SET current_value = current_value + step, version = version + 1 " +
                "WHERE business_key = ? AND version = ?";
        selectByBusinessKeySql = "SELECT * FROM " + tableName + " WHERE business_key = ?";
        insertBusinessKeySql = "INSERT INTO " + tableName + 
                "(business_key, current_value, step, version, create_time, update_time) " +
                "VALUES (?, ?, ?, 0, NOW(), NOW())";
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Segment getNextSegment(String businessKey, int step) {
        try {
            // 查询业务键是否存在
            return jdbcTemplate.query(selectByBusinessKeySql, rs -> {
                if (!rs.next()) {
                    throw new IdGeneratorException("业务键不存在: " + businessKey);
                }

                // 获取当前值和版本号
                long currentValue = rs.getLong("current_value");
                int version = rs.getInt("version");
                int stepValue = rs.getInt("step");
                
                // 如果传入了步长，则使用传入的步长，否则使用数据库中的步长
                int actualStep = step > 0 ? step : stepValue;

                // 更新下一个号段的值
                int updated = jdbcTemplate.update(updateNextValueSql, businessKey, version);
                if (updated != 1) {
                    throw new IdGeneratorException("并发更新冲突，获取号段失败");
                }

                // 创建号段对象
                Segment segment = new Segment();
                segment.setBusinessKey(businessKey);
                segment.setStep(actualStep);
                segment.setCurrentValue(new AtomicLong(currentValue));
                segment.setMaxValue(currentValue + actualStep);
                return segment;
            }, businessKey);
        } catch (DataAccessException e) {
            log.error("获取号段失败", e);
            throw new IdGeneratorException("获取号段失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void initBusinessKey(String businessKey, int step) {
        try {
            // 查询业务键是否存在
            boolean exists = jdbcTemplate.query(selectByBusinessKeySql, rs -> {
                return rs.next();
            }, businessKey);

            if (!exists) {
                // 不存在，初始化
                int inserted = jdbcTemplate.update(insertBusinessKeySql, businessKey, 0, step);
                if (inserted != 1) {
                    throw new IdGeneratorException("初始化业务键失败");
                }
                log.info("初始化业务键成功: {}", businessKey);
            } else {
                log.info("业务键已存在: {}", businessKey);
            }
        } catch (DataAccessException e) {
            log.error("初始化业务键失败", e);
            throw new IdGeneratorException("初始化业务键失败: " + e.getMessage(), e);
        }
    }
} 