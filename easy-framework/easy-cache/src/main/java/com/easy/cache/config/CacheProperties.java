package com.easy.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置属性，用于Spring Boot配置文件中设置
 */
@ConfigurationProperties(prefix = "easy.cache")
public class CacheProperties {
    
    /**
     * 是否启用缓存
     */
    private boolean enabled = true;
    
    /**
     * 缓存统计信息间隔（分钟）
     */
    private int statIntervalMinutes = 30;
    
    /**
     * 是否在缓存名称中包含区域前缀
     */
    private boolean areaInCacheName = false;
    
    /**
     * 本地缓存配置
     */
    private LocalCacheProperties local = new LocalCacheProperties();
    
    /**
     * Redis缓存配置
     */
    private RedisCacheProperties redis = new RedisCacheProperties();
    
    /**
     * 多级缓存配置
     */
    private MultiLevelCacheProperties multiLevel = new MultiLevelCacheProperties();
    
    /**
     * 本地缓存配置属性
     */
    public static class LocalCacheProperties {
        
        /**
         * 是否启用本地缓存
         */
        private boolean enabled = true;
        
        /**
         * 初始容量
         */
        private int initialCapacity = 100;
        
        /**
         * 最大容量
         */
        private int maximumSize = 10000;
        
        /**
         * 写入后过期时间
         */
        private long expireAfterWrite = 30;
        
        /**
         * 访问后过期时间
         */
        private long expireAfterAccess = 0;
        
        /**
         * 时间单位
         */
        private TimeUnit timeUnit = TimeUnit.MINUTES;
        
        /**
         * 缓存类型，默认Caffeine
         */
        private String type = "caffeine";
        
        /**
         * 键转换器类型
         */
        private String keyConvertor = "fastjson2";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getInitialCapacity() {
            return initialCapacity;
        }

        public void setInitialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
        }

        public int getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(int maximumSize) {
            this.maximumSize = maximumSize;
        }

        public long getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(long expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public long getExpireAfterAccess() {
            return expireAfterAccess;
        }

        public void setExpireAfterAccess(long expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getKeyConvertor() {
            return keyConvertor;
        }

        public void setKeyConvertor(String keyConvertor) {
            this.keyConvertor = keyConvertor;
        }
    }
    
    /**
     * Redis缓存配置属性
     */
    public static class RedisCacheProperties {
        
        /**
         * 是否启用Redis缓存
         */
        private boolean enabled = true;
        
        /**
         * Redis服务器主机
         */
        private String host = "localhost";
        
        /**
         * Redis服务器端口
         */
        private int port = 6379;
        
        /**
         * Redis密码
         */
        private String password;
        
        /**
         * 数据库索引
         */
        private int database = 0;
        
        /**
         * 连接超时（毫秒）
         */
        private int timeout = 2000;
        
        /**
         * 最大连接数
         */
        private int maxTotal = 8;
        
        /**
         * 最大空闲连接数
         */
        private int maxIdle = 8;
        
        /**
         * 最小空闲连接数
         */
        private int minIdle = 0;
        
        /**
         * 序列化器类型
         */
        private String serializer = "java";
        
        /**
         * 写入后过期时间
         */
        private long expireAfterWrite = 1;
        
        /**
         * 时间单位
         */
        private TimeUnit timeUnit = TimeUnit.HOURS;
        
        /**
         * 键转换器类型
         */
        private String keyConvertor = "fastjson2";
        
        /**
         * 广播通道名称
         */
        private String broadcastChannel = "easycache";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getMaxTotal() {
            return maxTotal;
        }

        public void setMaxTotal(int maxTotal) {
            this.maxTotal = maxTotal;
        }

        public int getMaxIdle() {
            return maxIdle;
        }

        public void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }

        public int getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        public String getSerializer() {
            return serializer;
        }

        public void setSerializer(String serializer) {
            this.serializer = serializer;
        }

        public long getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(long expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }

        public String getKeyConvertor() {
            return keyConvertor;
        }

        public void setKeyConvertor(String keyConvertor) {
            this.keyConvertor = keyConvertor;
        }

        public String getBroadcastChannel() {
            return broadcastChannel;
        }

        public void setBroadcastChannel(String broadcastChannel) {
            this.broadcastChannel = broadcastChannel;
        }
    }
    
    /**
     * 多级缓存配置属性
     */
    public static class MultiLevelCacheProperties {
        
        /**
         * 是否启用多级缓存
         */
        private boolean enabled = true;
        
        /**
         * 是否直写模式（修改时同时写入本地和远程缓存）
         */
        private boolean writeThrough = true;
        
        /**
         * 是否异步写入远程缓存
         */
        private boolean asyncWrite = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isWriteThrough() {
            return writeThrough;
        }

        public void setWriteThrough(boolean writeThrough) {
            this.writeThrough = writeThrough;
        }

        public boolean isAsyncWrite() {
            return asyncWrite;
        }

        public void setAsyncWrite(boolean asyncWrite) {
            this.asyncWrite = asyncWrite;
        }
    }

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getStatIntervalMinutes() {
        return statIntervalMinutes;
    }

    public void setStatIntervalMinutes(int statIntervalMinutes) {
        this.statIntervalMinutes = statIntervalMinutes;
    }

    public boolean isAreaInCacheName() {
        return areaInCacheName;
    }

    public void setAreaInCacheName(boolean areaInCacheName) {
        this.areaInCacheName = areaInCacheName;
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

    public MultiLevelCacheProperties getMultiLevel() {
        return multiLevel;
    }

    public void setMultiLevel(MultiLevelCacheProperties multiLevel) {
        this.multiLevel = multiLevel;
    }
} 