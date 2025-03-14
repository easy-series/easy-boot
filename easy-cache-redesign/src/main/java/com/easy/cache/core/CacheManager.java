package com.easy.cache.core;

import com.easy.cache.sync.CacheSyncManager;
import com.easy.cache.util.Serializer;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器，负责创建和管理缓存实例
 */
public class CacheManager {

    /**
     * 单例实例
     */
    private static final CacheManager INSTANCE = new CacheManager();

    /**
     * 缓存实例存储
     */
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * Redis模板
     */
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 序列化器
     */
    private Serializer serializer;

    /**
     * 本地缓存最大容量
     */
    private int localCacheMaximumSize = 10000;

    /**
     * 本地缓存初始容量
     */
    private int localCacheInitialCapacity = 16;

    /**
     * 默认本地缓存过期时间
     */
    private long defaultLocalExpiration = 0;

    /**
     * 默认本地缓存过期时间单位
     */
    private TimeUnit defaultLocalTimeUnit = TimeUnit.SECONDS;

    /**
     * 默认Redis缓存过期时间
     */
    private long defaultRedisExpiration = 0;

    /**
     * 默认Redis缓存过期时间单位
     */
    private TimeUnit defaultRedisTimeUnit = TimeUnit.SECONDS;

    /**
     * 多级缓存是否写透
     */
    private boolean writeThrough = true;

    /**
     * 多级缓存是否异步写入
     */
    private boolean asyncWrite = false;

    /**
     * 私有构造函数
     */
    private CacheManager() {
    }

    /**
     * 获取单例实例
     *
     * @return 缓存管理器实例
     */
    public static CacheManager getInstance() {
        return INSTANCE;
    }

    /**
     * 设置Redis模板
     *
     * @param redisTemplate Redis模板
     */
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
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
     * 设置本地缓存最大容量
     *
     * @param localCacheMaximumSize 最大容量
     */
    public void setLocalCacheMaximumSize(int localCacheMaximumSize) {
        this.localCacheMaximumSize = localCacheMaximumSize;
    }

    /**
     * 设置本地缓存初始容量
     *
     * @param localCacheInitialCapacity 初始容量
     */
    public void setLocalCacheInitialCapacity(int localCacheInitialCapacity) {
        this.localCacheInitialCapacity = localCacheInitialCapacity;
    }

    /**
     * 设置默认本地缓存过期时间
     *
     * @param defaultLocalExpiration 过期时间
     */
    public void setDefaultLocalExpiration(long defaultLocalExpiration) {
        this.defaultLocalExpiration = defaultLocalExpiration;
    }

    /**
     * 设置默认本地缓存过期时间单位
     *
     * @param defaultLocalTimeUnit 时间单位
     */
    public void setDefaultLocalTimeUnit(TimeUnit defaultLocalTimeUnit) {
        this.defaultLocalTimeUnit = defaultLocalTimeUnit;
    }

    /**
     * 设置默认Redis缓存过期时间
     *
     * @param defaultRedisExpiration 过期时间
     */
    public void setDefaultRedisExpiration(long defaultRedisExpiration) {
        this.defaultRedisExpiration = defaultRedisExpiration;
    }

    /**
     * 设置默认Redis缓存过期时间单位
     *
     * @param defaultRedisTimeUnit 时间单位
     */
    public void setDefaultRedisTimeUnit(TimeUnit defaultRedisTimeUnit) {
        this.defaultRedisTimeUnit = defaultRedisTimeUnit;
    }

    /**
     * 设置多级缓存是否写透
     *
     * @param writeThrough 是否写透
     */
    public void setWriteThrough(boolean writeThrough) {
        this.writeThrough = writeThrough;
    }

    /**
     * 设置多级缓存是否异步写入
     *
     * @param asyncWrite 是否异步写入
     */
    public void setAsyncWrite(boolean asyncWrite) {
        this.asyncWrite = asyncWrite;
    }

    /**
     * 获取或创建本地缓存
     *
     * @param name 缓存名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 本地缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateLocalCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(name + ":local",
                k -> {
                    LocalCache<K, V> cache = new LocalCache<>(name, localCacheInitialCapacity, localCacheMaximumSize);
                    if (defaultLocalExpiration > 0) {
                        // 设置默认过期时间
                        cache.put("__dummy", null, defaultLocalExpiration, defaultLocalTimeUnit);
                    }
                    return cache;
                });
    }

    /**
     * 获取或创建Redis缓存
     *
     * @param name 缓存名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return Redis缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateRedisCache(String name) {
        if (redisTemplate == null) {
            throw new IllegalStateException("RedisTemplate未设置");
        }
        if (serializer == null) {
            throw new IllegalStateException("Serializer未设置");
        }

        return (Cache<K, V>) caches.computeIfAbsent(name + ":redis",
                k -> {
                    RedisCache<K, V> cache = new RedisCache<>(name, redisTemplate, serializer);
                    if (defaultRedisExpiration > 0) {
                        cache.setExpire(defaultRedisExpiration, defaultRedisTimeUnit);
                    }
                    return cache;
                });
    }

    /**
     * 获取或创建两级缓存（本地缓存+Redis缓存）
     *
     * @param name 缓存名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 多级缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateTwoLevelCache(String name) {
        return getOrCreateTwoLevelCache(name, writeThrough, asyncWrite);
    }

    /**
     * 获取或创建两级缓存（本地缓存+Redis缓存）
     *
     * @param name         缓存名称
     * @param writeThrough 是否写透
     * @param asyncWrite   是否异步写入
     * @param <K>          键类型
     * @param <V>          值类型
     * @return 多级缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateTwoLevelCache(String name, boolean writeThrough, boolean asyncWrite) {
        if (redisTemplate == null) {
            throw new IllegalStateException("RedisTemplate未设置");
        }
        if (serializer == null) {
            throw new IllegalStateException("Serializer未设置");
        }

        return (Cache<K, V>) caches.computeIfAbsent(name + ":two-level",
                k -> {
                    Cache<K, V> localCache = getOrCreateLocalCache(name + ":local");
                    Cache<K, V> redisCache = getOrCreateRedisCache(name + ":redis");
                    return new MultiLevelCache<>(name, Arrays.asList(localCache, redisCache), writeThrough, asyncWrite);
                });
    }

    /**
     * 获取缓存
     *
     * @param name 缓存名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 缓存实例，如果不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.get(name);
    }

    /**
     * 移除缓存
     *
     * @param name 缓存名称
     * @return 如果缓存存在并被移除则返回true，否则返回false
     */
    public boolean removeCache(String name) {
        Cache<?, ?> cache = caches.remove(name);
        if (cache instanceof MultiLevelCache) {
            ((MultiLevelCache<?, ?>) cache).shutdown();
        }
        return cache != null;
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        caches.values().forEach(Cache::clear);
    }

    /**
     * 初始化缓存同步
     */
    public void initCacheSync() {
        if (redisTemplate == null) {
            throw new IllegalStateException("RedisTemplate未设置");
        }
        if (serializer == null) {
            throw new IllegalStateException("Serializer未设置");
        }

        CacheSyncManager.getInstance().init(redisTemplate, serializer);
    }

    /**
     * 关闭缓存管理器
     */
    public void shutdown() {
        // 关闭所有多级缓存
        caches.values().forEach(cache -> {
            if (cache instanceof MultiLevelCache) {
                ((MultiLevelCache<?, ?>) cache).shutdown();
            }
        });

        // 关闭缓存同步管理器
        CacheSyncManager.getInstance().shutdown();

        // 清空所有缓存
        caches.clear();
    }
}