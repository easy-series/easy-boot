package com.easy.cache.core;

import com.easy.cache.config.CacheProperties;
import com.easy.cache.event.CacheSyncManager;
import com.easy.cache.serializer.JdkSerializer;
import com.easy.cache.serializer.JsonSerializer;
import com.easy.cache.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器，负责管理所有缓存实例
 */
public class CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

    /**
     * 缓存属性配置
     */
    private final CacheProperties cacheProperties;

    /**
     * Redis模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存同步管理器
     */
    private CacheSyncManager syncManager;

    /**
     * 缓存映射表
     */
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * 本地缓存初始容量
     */
    private int localInitialCapacity = 100;

    /**
     * 本地缓存最大容量
     */
    private long localMaximumSize = 10000;

    /**
     * 本地缓存过期时间
     */
    private long localExpireAfterWrite = 30;

    /**
     * 本地缓存时间单位
     */
    private TimeUnit localTimeUnit = TimeUnit.MINUTES;

    /**
     * Redis缓存过期时间
     */
    private long redisExpireAfterWrite = 60;

    /**
     * Redis缓存时间单位
     */
    private TimeUnit redisTimeUnit = TimeUnit.MINUTES;

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
     * 构造函数
     *
     * @param cacheProperties 缓存属性配置
     * @param redisTemplate   Redis模板
     */
    public CacheManager(CacheProperties cacheProperties, RedisTemplate<String, Object> redisTemplate) {
        this.cacheProperties = cacheProperties;
        this.redisTemplate = redisTemplate;

        // 初始化本地缓存配置
        this.localInitialCapacity = cacheProperties.getLocal().getInitialCapacity();
        this.localMaximumSize = cacheProperties.getLocal().getMaximumSize();
        this.localExpireAfterWrite = cacheProperties.getLocal().getExpireAfterWrite();
        this.localTimeUnit = cacheProperties.getLocal().getTimeUnit();

        // 初始化Redis缓存配置
        this.redisExpireAfterWrite = cacheProperties.getRedis().getExpireAfterWrite();
        this.redisTimeUnit = cacheProperties.getRedis().getTimeUnit();
        this.serializerType = cacheProperties.getRedis().getSerializer();

        // 初始化多级缓存配置
        this.writeThrough = cacheProperties.getMultiLevel().isWriteThrough();
        this.asyncWrite = cacheProperties.getMultiLevel().isAsyncWrite();
        
        // 初始化缓存同步配置
        this.syncEnabled = cacheProperties.getSync().isEnabled();
    }

    /**
     * 获取指定名称的缓存
     *
     * @param <K>      键类型
     * @param <V>      值类型
     * @param name     缓存名称
     * @param cacheType 缓存类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name, CacheType cacheType) {
        Cache<K, V> cache = (Cache<K, V>) caches.get(name);
        
        if (cache == null) {
            synchronized (caches) {
                cache = (Cache<K, V>) caches.get(name);
                
                if (cache == null) {
                    // 创建新的缓存实例
                    cache = createCache(name, cacheType);
                    caches.put(name, cache);
                    logger.info("Created new cache: {} of type: {}", name, cacheType);
                }
            }
        }
        
        return cache;
    }

    /**
     * 创建新的缓存实例
     *
     * @param <K>      键类型
     * @param <V>      值类型
     * @param name     缓存名称
     * @param cacheType 缓存类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> createCache(String name, CacheType cacheType) {
        CacheBuilder<K, V> builder = CacheBuilder.newBuilder()
                .name(name)
                .cacheType(cacheType)
                .initialCapacity(localInitialCapacity)
                .maximumSize(localMaximumSize)
                .expireAfterWrite(
                        cacheType == CacheType.REDIS ? redisExpireAfterWrite : localExpireAfterWrite,
                        cacheType == CacheType.REDIS ? redisTimeUnit : localTimeUnit
                )
                .writeThrough(writeThrough)
                .asyncWrite(asyncWrite);
        
        // 如果使用Redis或多级缓存，需要设置Redis相关配置
        if (cacheType == CacheType.REDIS || cacheType == CacheType.TWO_LEVEL) {
            builder.redisTemplate(redisTemplate);
            
            if (serializer != null) {
                builder.serializer(serializer);
            } else {
                builder.serializerType(serializerType);
            }
        }
        
        // 如果启用加密，设置加密配置
        if (encrypted && encryptionKey != null && !encryptionKey.isEmpty()) {
            builder.encrypted(true, encryptionKey);
        }
        
        // 如果启用缓存同步，设置同步配置
        if (syncEnabled && syncManager != null) {
            builder.syncEnabled(true, syncManager);
        }
        
        return builder.build();
    }
    
    /**
     * 设置缓存同步管理器
     *
     * @param syncManager 缓存同步管理器
     */
    public void setSyncManager(CacheSyncManager syncManager) {
        this.syncManager = syncManager;
    }

    // 省略getters和setters方法...

    // 返回缓存数量
    public int size() {
        return caches.size();
    }

    // 清空所有缓存
    public void clear() {
        synchronized (caches) {
            for (Cache<?, ?> cache : caches.values()) {
                cache.clear();
            }
            caches.clear();
        }
    }

    // 移除指定名称的缓存
    public void removeCache(String name) {
        synchronized (caches) {
            Cache<?, ?> cache = caches.remove(name);
            if (cache != null) {
                cache.clear();
                logger.info("Removed cache: {}", name);
            }
        }
    }

    /**
     * 获取序列化器
     *
     * @return 序列化器
     */
    public Serializer getSerializer() {
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
     * 设置序列化器
     *
     * @param serializer 序列化器
     */
    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    /**
     * 设置本地缓存初始容量
     */
    public void setLocalInitialCapacity(int localInitialCapacity) {
        this.localInitialCapacity = localInitialCapacity;
    }

    /**
     * 设置本地缓存最大容量
     */
    public void setLocalMaximumSize(long localMaximumSize) {
        this.localMaximumSize = localMaximumSize;
    }

    /**
     * 设置本地缓存过期时间
     */
    public void setLocalExpireAfterWrite(long localExpireAfterWrite) {
        this.localExpireAfterWrite = localExpireAfterWrite;
    }

    /**
     * 设置本地缓存时间单位
     */
    public void setLocalTimeUnit(TimeUnit localTimeUnit) {
        this.localTimeUnit = localTimeUnit;
    }

    /**
     * 设置Redis缓存过期时间
     */
    public void setRedisExpireAfterWrite(long redisExpireAfterWrite) {
        this.redisExpireAfterWrite = redisExpireAfterWrite;
    }

    /**
     * 设置Redis缓存时间单位
     */
    public void setRedisTimeUnit(TimeUnit redisTimeUnit) {
        this.redisTimeUnit = redisTimeUnit;
    }

    /**
     * 设置序列化器类型
     */
    public void setSerializerType(String serializerType) {
        this.serializerType = serializerType;
    }

    /**
     * 设置是否写穿透
     */
    public void setWriteThrough(boolean writeThrough) {
        this.writeThrough = writeThrough;
    }

    /**
     * 设置是否异步写入
     */
    public void setAsyncWrite(boolean asyncWrite) {
        this.asyncWrite = asyncWrite;
    }

    /**
     * 设置是否使用加密
     */
    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    /**
     * 设置加密密钥
     */
    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    /**
     * 设置是否启用缓存同步
     */
    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }
} 