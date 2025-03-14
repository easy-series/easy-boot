package com.easy.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置属性类
 */
@ConfigurationProperties(prefix = "easy.cache")
public class CacheProperties {

    /**
     * 是否启用缓存
     */
    private boolean enabled = true;

    /**
     * 默认序列化器类型：jdk 或 json
     */
    private String serializer = "jdk";

    /**
     * 默认缓存类型：local、redis 或 multilevel
     */
    private String type = "local";

    /**
     * 本地缓存配置
     */
    private LocalCacheProperties local = new LocalCacheProperties();

    /**
     * Redis缓存配置
     */
    private RedisCacheProperties redis = new RedisCacheProperties();

    /**
     * 缓存同步配置
     */
    private SyncProperties sync = new SyncProperties();

    /**
     * 自定义缓存配置
     */
    private Map<String, CustomCacheProperties> caches = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSerializer() {
        return serializer;
    }

    public void setSerializer(String serializer) {
        this.serializer = serializer;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalCacheProperties getLocal() {
        return local;
    }

    public void setLocal(LocalCacheProperties local) {
        this.local = local;
    }

    public RedisCacheProperties getRedis() {
        return redis;
    }

    public void setRedis(RedisCacheProperties redis) {
        this.redis = redis;
    }

    public SyncProperties getSync() {
        return sync;
    }

    public void setSync(SyncProperties sync) {
        this.sync = sync;
    }

    public Map<String, CustomCacheProperties> getCaches() {
        return caches;
    }

    public void setCaches(Map<String, CustomCacheProperties> caches) {
        this.caches = caches;
    }

    /**
     * 本地缓存配置属性
     */
    public static class LocalCacheProperties {
        /**
         * 最大缓存条目数
         */
        private int maxSize = 1000;

        /**
         * 缓存过期时间（秒）
         */
        private int expireAfterWrite = 3600;

        /**
         * 缓存刷新时间（秒）
         */
        private int refreshAfterWrite = 0;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(int expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public int getRefreshAfterWrite() {
            return refreshAfterWrite;
        }

        public void setRefreshAfterWrite(int refreshAfterWrite) {
            this.refreshAfterWrite = refreshAfterWrite;
        }
    }

    /**
     * Redis缓存配置属性
     */
    public static class RedisCacheProperties {
        /**
         * 键前缀
         */
        private String keyPrefix = "easy:cache:";

        /**
         * 缓存过期时间（秒）
         */
        private int expireAfterWrite = 3600;

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public int getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(int expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }
    }

    /**
     * 缓存同步配置属性
     */
    public static class SyncProperties {
        /**
         * 是否启用缓存同步
         */
        private boolean enabled = false;

        /**
         * 同步策略：invalidate 或 update
         */
        private String strategy = "invalidate";

        /**
         * 主题前缀
         */
        private String topicPrefix = "easy:cache:sync:";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public String getTopicPrefix() {
            return topicPrefix;
        }

        public void setTopicPrefix(String topicPrefix) {
            this.topicPrefix = topicPrefix;
        }
    }

    /**
     * 自定义缓存配置属性
     */
    public static class CustomCacheProperties {
        /**
         * 缓存类型：local、redis 或 multilevel
         */
        private String type;

        /**
         * 序列化器类型：jdk 或 json
         */
        private String serializer;

        /**
         * 最大缓存条目数（本地缓存）
         */
        private Integer maxSize;

        /**
         * 缓存过期时间（秒）
         */
        private Integer expireAfterWrite;

        /**
         * 缓存刷新时间（秒，本地缓存）
         */
        private Integer refreshAfterWrite;

        /**
         * 键前缀（Redis缓存）
         */
        private String keyPrefix;

        /**
         * 是否启用缓存同步
         */
        private Boolean syncEnabled;

        /**
         * 同步策略：invalidate 或 update
         */
        private String syncStrategy;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSerializer() {
            return serializer;
        }

        public void setSerializer(String serializer) {
            this.serializer = serializer;
        }

        public Integer getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(Integer maxSize) {
            this.maxSize = maxSize;
        }

        public Integer getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(Integer expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public Integer getRefreshAfterWrite() {
            return refreshAfterWrite;
        }

        public void setRefreshAfterWrite(Integer refreshAfterWrite) {
            this.refreshAfterWrite = refreshAfterWrite;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public Boolean getSyncEnabled() {
            return syncEnabled;
        }

        public void setSyncEnabled(Boolean syncEnabled) {
            this.syncEnabled = syncEnabled;
        }

        public String getSyncStrategy() {
            return syncStrategy;
        }

        public void setSyncStrategy(String syncStrategy) {
            this.syncStrategy = syncStrategy;
        }
    }
}