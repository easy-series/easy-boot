package com.easy.id.segment.dao;

import com.easy.id.exception.IdGeneratorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * 基于数据库的号段分配器
 * 
 * 使用行锁和事务来确保号段分配的原子性
 *
 * @author 芋道源码
 */
@Slf4j
public class DbSegmentAllocator implements SegmentAllocator {

    /**
     * JDBC操作模板
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 事务管理器
     */
    private final PlatformTransactionManager transactionManager;

    /**
     * 表名
     */
    private final String tableName;

    /**
     * 构造函数
     * 
     * @param dataSource         数据源
     * @param transactionManager 事务管理器
     * @param tableName          表名
     */
    public DbSegmentAllocator(DataSource dataSource, PlatformTransactionManager transactionManager, String tableName) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionManager = transactionManager;
        this.tableName = tableName;
    }

    @Override
    public SegmentRange nextRange(String bizKey, int step) {
        // 创建事务定义，使用REQUIRES_NEW隔离级别
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            // 查询业务记录，加行锁
            String selectSql = "SELECT max_id, step, version FROM " + tableName + " WHERE biz_key = ? FOR UPDATE";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(selectSql, bizKey);

            long newMaxId;
            int version;

            if (result.isEmpty()) {
                // 记录不存在，初始化记录
                newMaxId = step;
                version = 1;
                String insertSql = "INSERT INTO " + tableName +
                        " (biz_key, max_id, step, version, update_time, create_time) VALUES (?, ?, ?, ?, ?, ?)";

                Timestamp now = new Timestamp(System.currentTimeMillis());
                jdbcTemplate.update(insertSql, bizKey, newMaxId, step, version, now, now);
            } else {
                // 记录存在，更新记录
                Map<String, Object> row = result.get(0);
                long maxId = ((Number) row.get("max_id")).longValue();
                int dbStep = row.get("step") != null ? ((Number) row.get("step")).intValue() : step;
                version = ((Number) row.get("version")).intValue();

                // 使用传入的步长或数据库中的步长
                int finalStep = step > 0 ? step : dbStep;

                // 计算新的最大ID
                newMaxId = maxId + finalStep;

                // 更新记录
                String updateSql = "UPDATE " + tableName +
                        " SET max_id = ?, step = ?, version = ?, update_time = ? WHERE biz_key = ? AND version = ?";
                int updated = jdbcTemplate.update(
                        updateSql,
                        newMaxId,
                        finalStep,
                        version + 1,
                        new Timestamp(System.currentTimeMillis()),
                        bizKey,
                        version);

                if (updated != 1) {
                    throw new IdGeneratorException("更新号段失败，可能存在并发更新，bizKey: " + bizKey);
                }
            }

            // 提交事务
            transactionManager.commit(status);

            // 返回号段范围
            long minId = newMaxId - step + 1;
            return new SegmentRange(minId, newMaxId, step);
        } catch (DataAccessException e) {
            // 数据库访问异常，回滚事务
            transactionManager.rollback(status);
            log.error("从数据库获取号段失败，bizKey: {}", bizKey, e);
            throw new IdGeneratorException("从数据库获取号段失败: " + e.getMessage(), e);
        } catch (Exception e) {
            // 其他异常，回滚事务
            transactionManager.rollback(status);
            log.error("从数据库获取号段时发生异常，bizKey: {}", bizKey, e);
            throw new IdGeneratorException("从数据库获取号段时发生异常: " + e.getMessage(), e);
        }
    }

    /**
     * 检查表是否存在，如果不存在则创建表
     */
    public void init() {
        try {
            // 检查表是否存在
            String checkSql = "SELECT COUNT(*) FROM " + tableName + " LIMIT 1";
            try {
                jdbcTemplate.queryForObject(checkSql, Integer.class);
                log.info("ID分配表已存在: {}", tableName);
            } catch (DataAccessException e) {
                // 表不存在，创建表
                log.info("ID分配表不存在，正在创建: {}", tableName);
                String createSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n" +
                        "  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',\n" +
                        "  biz_key VARCHAR(128) NOT NULL COMMENT '业务键',\n" +
                        "  max_id BIGINT NOT NULL COMMENT '当前最大ID',\n" +
                        "  step INT NOT NULL COMMENT '步长',\n" +
                        "  version INT NOT NULL COMMENT '版本号',\n" +
                        "  description VARCHAR(256) DEFAULT NULL COMMENT '描述',\n" +
                        "  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',\n" +
                        "  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n" +
                        "  PRIMARY KEY (id),\n" +
                        "  UNIQUE KEY uk_biz_key (biz_key)\n" +
                        ");";
                jdbcTemplate.execute(createSql);
                log.info("ID分配表创建成功: {}", tableName);
            }
        } catch (Exception e) {
            log.error("初始化ID分配表失败", e);
            throw new IdGeneratorException("初始化ID分配表失败: " + e.getMessage(), e);
        }
    }
}