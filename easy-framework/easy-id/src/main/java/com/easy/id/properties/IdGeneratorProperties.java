package com.easy.id.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * ID生成器配置属性
 */
@Data
@ConfigurationProperties(prefix = "id-generator")
public class IdGeneratorProperties {

    /**
     * ID生成器类型
     * - snowflake: 雪花算法
     * - segment-db: 基于数据库的号段模式
     * - segment-redis: 基于Redis的号段模式
     */
    private String type = "snowflake";

    /**
     * 雪花算法配置
     */
    private SnowflakeProperties snowflake = new SnowflakeProperties();

    /**
     * 号段模式配置
     */
    private SegmentProperties segment = new SegmentProperties();

    /**
     * 雪花算法配置属性
     */
    @Data
    public static class SnowflakeProperties {
        /**
         * 机器ID (0-1023)
         */
        private long workerId = 0;
    }

    /**
     * 号段模式配置属性
     */
    @Data
    public static class SegmentProperties {
        /**
         * 号段步长
         */
        private int step = 1000;

        /**
         * 基于数据库的号段配置
         */
        private DbSegmentProperties db = new DbSegmentProperties();

        /**
         * 基于Redis的号段配置
         */
        private RedisSegmentProperties redis = new RedisSegmentProperties();
    }

    /**
     * 基于数据库的号段配置属性
     */
    @Data
    public static class DbSegmentProperties {
        /**
         * 表名
         */
        private String tableName = "id_generator";
    }

    /**
     * 基于Redis的号段配置属性
     */
    @Data
    public static class RedisSegmentProperties {
        /**
         * Redis键前缀
         */
        private String keyPrefix = "easy:id";
    }
}