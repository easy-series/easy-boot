package com.easy.id.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * ID生成器配置属性
 *
 * @author 芋道源码
 */
@Data
@ConfigurationProperties(prefix = "easy.id")
public class IdProperties {

    /**
     * 默认的生成器类型
     */
    private String defaultType = "snowflake";

    /**
     * 雪花算法配置
     */
    private SnowflakeProperties snowflake = new SnowflakeProperties();

    /**
     * 号段模式配置
     */
    private SegmentProperties segment = new SegmentProperties();

    /**
     * Redis号段模式配置
     */
    private RedisSegmentProperties redisSegment = new RedisSegmentProperties();

    /**
     * 雪花算法配置属性
     */
    @Data
    public static class SnowflakeProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 数据中心ID
         */
        private Long dataCenterId = 1L;

        /**
         * 工作节点ID
         */
        private Long workerId = 1L;

        /**
         * 生成器名称
         */
        private String name = "snowflake";
    }

    /**
     * 号段模式配置属性
     */
    @Data
    public static class SegmentProperties {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * 数据库表名
         */
        private String tableName = "easy_id_allocator";

        /**
         * 业务配置
         */
        private Map<String, BizConfig> bizConfigs = new HashMap<>();

        /**
         * 生成器名称
         */
        private String name = "segment";
    }

    /**
     * Redis号段模式配置属性
     */
    @Data
    public static class RedisSegmentProperties {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * Redis键前缀
         */
        private String keyPrefix = "easy:id:segment:";

        /**
         * 业务配置
         */
        private Map<String, BizConfig> bizConfigs = new HashMap<>();

        /**
         * 生成器名称
         */
        private String name = "redis-segment";
    }

    /**
     * 业务配置
     */
    @Data
    public static class BizConfig {
        /**
         * 业务键
         */
        private String bizKey;

        /**
         * 步长
         */
        private int step = 1000;

        /**
         * 描述
         */
        private String description;
    }
} 