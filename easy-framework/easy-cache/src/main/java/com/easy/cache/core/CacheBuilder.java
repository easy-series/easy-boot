package com.easy.cache.core;

import com.easy.cache.event.CacheSyncManager;
import com.easy.cache.serializer.JdkSerializer;
import com.easy.cache.serializer.JsonSerializer;
import com.easy.cache.serializer.Serializer;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 缓存构建器，用于构建不同类型的缓存
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class CacheBuilder<K, V> {

    /**
     * 缓存名称
     */
    private String name;
    
    /**
     * 缓存类型
     */
    private CacheType cacheType = CacheType.LOCAL;
    
    /**
     * 初始容量
     */
    private int initialCapacity = 100;
    
    /**
     * 最大容量
     */
    private long maximumSize = 10000;
    
    /**
     * 过期时间
     */
    private long expireAfterWrite = 30;
    
    /**
     * 时间单位
     */
    private TimeUnit timeUnit = TimeUnit.MINUTES;
    
    /**
     * Redis模板
     */
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 序列化器类型
     */
    private String serializerType = "JDK";
    
    /**
     * 自定义序列化器
     */
    private Serializer serializer;
    
    /**
     * 是否写穿透
     */
    private boolean writeThrough = true;
    
    /**
     * 是否异步写入
     */
    private boolean asyncWrite = false;
    
    /**
     * 是否使用加密
     */
    private boolean encrypted = false;
    
    /**
     * 加密密钥
     */
    private String encryptionKey;
    
    /**
     * 是否启用缓存同步
     */
    private boolean syncEnabled = false;
    
    /**
     * 缓存同步管理器
     */
    private CacheSyncManager syncManager;

    /**
     * 创建一个新的缓存构建器实例
     */
    private CacheBuilder() {
    }

    /**
     * 创建一个新的缓存构建器
     *
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 缓存构建器实例
     */
    public static <K, V> CacheBuilder<K, V> newBuilder() {
        return new CacheBuilder<>();
    }
    
    /**
     * 设置缓存名称
     *
     * @param name 缓存名称
     * @return 当前构建器
     */
    public CacheBuilder<K, V> name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * 设置缓存类型
     *
     * @param cacheType 缓存类型
     * @return 当前构建器
     */
    public CacheBuilder<K, V> cacheType(CacheType cacheType) {
        this.cacheType = cacheType;
        return this;
    }
    
    /**
     * 设置初始容量
     *
     * @param initialCapacity 初始容量
     * @return 当前构建器
     */
    public CacheBuilder<K, V> initialCapacity(int initialCapacity) {
        this.initialCapacity = initialCapacity;
        return this;
    }
    
    /**
     * 设置最大容量
     *
     * @param maximumSize 最大容量
     * @return 当前构建器
     */
    public CacheBuilder<K, V> maximumSize(long maximumSize) {
        this.maximumSize = maximumSize;
        return this;
    }
    
    /**
     * 设置过期时间
     *
     * @param expireAfterWrite 过期时间
     * @param timeUnit 时间单位
     * @return 当前构建器
     */
    public CacheBuilder<K, V> expireAfterWrite(long expireAfterWrite, TimeUnit timeUnit) {
        this.expireAfterWrite = expireAfterWrite;
        this.timeUnit = timeUnit;
        return this;
    }
    
    /**
     * 设置Redis模板
     *
     * @param redisTemplate Redis模板
     * @return 当前构建器
     */
    public CacheBuilder<K, V> redisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        return this;
    }
    
    /**
     * 设置序列化器类型
     *
     * @param serializerType 序列化器类型
     * @return 当前构建器
     */
    public CacheBuilder<K, V> serializerType(String serializerType) {
        this.serializerType = serializerType;
        return this;
    }
    
    /**
     * 设置自定义序列化器
     *
     * @param serializer 自定义序列化器
     * @return 当前构建器
     */
    public CacheBuilder<K, V> serializer(Serializer serializer) {
        this.serializer = serializer;
        return this;
    }
    
    /**
     * 设置是否写穿透
     *
     * @param writeThrough 是否写穿透
     * @return 当前构建器
     */
    public CacheBuilder<K, V> writeThrough(boolean writeThrough) {
        this.writeThrough = writeThrough;
        return this;
    }
    
    /**
     * 设置是否异步写入
     *
     * @param asyncWrite 是否异步写入
     * @return 当前构建器
     */
    public CacheBuilder<K, V> asyncWrite(boolean asyncWrite) {
        this.asyncWrite = asyncWrite;
        return this;
    }
    
    /**
     * 设置是否使用加密
     *
     * @param encrypted 是否使用加密
     * @param encryptionKey 加密密钥
     * @return 当前构建器
     */
    public CacheBuilder<K, V> encrypted(boolean encrypted, String encryptionKey) {
        this.encrypted = encrypted;
        this.encryptionKey = encryptionKey;
        return this;
    }
    
    /**
     * 设置是否启用缓存同步
     *
     * @param syncEnabled 是否启用缓存同步
     * @param syncManager 缓存同步管理器
     * @return 当前构建器
     */
    public CacheBuilder<K, V> syncEnabled(boolean syncEnabled, CacheSyncManager syncManager) {
        this.syncEnabled = syncEnabled;
        this.syncManager = syncManager;
        return this;
    }
    
    /**
     * 获取序列化器
     *
     * @return 序列化器
     */
    private Serializer getSerializer() {
        if (serializer != null) {
            return serializer;
        }
        
        if ("JSON".equalsIgnoreCase(serializerType)) {
            return new JsonSerializer();
        } else {
            return new JdkSerializer();
        }
    }
    
    /**
     * 构建缓存
     *
     * @return 缓存实例
     */
    public Cache<K, V> build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Cache name cannot be null or empty");
        }
        
        Cache<K, V> cache;
        
        switch (cacheType) {
            case LOCAL:
                cache = new LocalCache<>(name, initialCapacity, maximumSize, expireAfterWrite, timeUnit);
                break;
            case REDIS:
                if (redisTemplate == null) {
                    throw new IllegalArgumentException("RedisTemplate is required for REDIS cache type");
                }
                
                cache = new RedisCache<>(name, redisTemplate, getSerializer(), expireAfterWrite, timeUnit);
                break;
            case TWO_LEVEL:
                if (redisTemplate == null) {
                    throw new IllegalArgumentException("RedisTemplate is required for TWO_LEVEL cache type");
                }
                
                Cache<K, V> localCache = new LocalCache<>(name + "_local", initialCapacity, maximumSize, expireAfterWrite, timeUnit);
                Cache<K, V> remoteCache = new RedisCache<>(name, redisTemplate, getSerializer(), expireAfterWrite, timeUnit);
                
                cache = new MultiLevelCache<>(name, localCache, remoteCache, writeThrough, asyncWrite);
                break;
            default:
                throw new IllegalArgumentException("Unsupported cache type: " + cacheType);
        }
        
        // TODO: 如果需要加密，包装成加密缓存
        
        // 如果启用缓存同步，包装成同步缓存
        if (syncEnabled && syncManager != null) {
            cache = new SyncCache<>(cache, syncManager);
        }
        
        return cache;
    }
} 