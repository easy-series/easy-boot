package com.easy.cache.core;

import com.easy.cache.core.RedisCache.Serializer;
import com.easy.cache.sync.CacheSyncManager;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器，用于创建和管理缓存实例
 */
public class CacheManager {

    private static final CacheManager INSTANCE = new CacheManager();

    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private JedisPool jedisPool;
    private Serializer serializer;

    private CacheManager() {
    }

    /**
     * 获取缓存管理器实例
     */
    public static CacheManager getInstance() {
        return INSTANCE;
    }

    /**
     * 设置Redis连接池
     */
    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 设置序列化器
     */
    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    /**
     * 获取Redis连接池
     * 
     * @return Redis连接池
     */
    public JedisPool getJedisPool() {
        return jedisPool;
    }

    /**
     * 获取序列化器
     * 
     * @return 序列化器
     */
    public Serializer getSerializer() {
        return serializer;
    }

    /**
     * 创建或获取本地缓存
     * 
     * @param name 缓存名称
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateLocalCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(name, k -> new LocalCache<K, V>(name));
    }

    /**
     * 创建或获取Redis缓存
     * 
     * @param name 缓存名称
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateRedisCache(String name) {
        if (jedisPool == null) {
            throw new IllegalStateException("JedisPool is not set");
        }
        if (serializer == null) {
            throw new IllegalStateException("Serializer is not set");
        }

        return (Cache<K, V>) caches.computeIfAbsent(name + ":redis",
                k -> new RedisCache<K, V>(name, jedisPool, serializer));
    }

    /**
     * 创建或获取多级缓存
     * 
     * @param name         缓存名称
     * @param writeThrough 是否写透
     * @param asyncWrite   是否异步写入
     * @param syncLocal    是否同步本地缓存
     * @param caches       缓存列表，按照优先级排序
     * @return 多级缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateMultiLevelCache(String name, boolean writeThrough, boolean asyncWrite,
            boolean syncLocal, Cache<K, V>... caches) {
        if (caches == null || caches.length == 0) {
            throw new IllegalArgumentException("Caches cannot be null or empty");
        }

        List<Cache<K, V>> cacheList = Arrays.asList(caches);
        return (Cache<K, V>) this.caches.computeIfAbsent(name + ":multi",
                k -> new MultiLevelCache<K, V>(name, cacheList, writeThrough, asyncWrite, syncLocal));
    }

    /**
     * 创建或获取二级缓存（本地缓存 + Redis缓存）
     * 
     * @param name         缓存名称
     * @param writeThrough 是否写透
     * @param asyncWrite   是否异步写入
     * @param syncLocal    是否同步本地缓存
     * @return 二级缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateTwoLevelCache(String name, boolean writeThrough, boolean asyncWrite,
            boolean syncLocal) {
        if (jedisPool == null) {
            throw new IllegalStateException("JedisPool is not set");
        }
        if (serializer == null) {
            throw new IllegalStateException("Serializer is not set");
        }

        Cache<K, V> localCache = getOrCreateLocalCache(name + ":local");
        Cache<K, V> redisCache = getOrCreateRedisCache(name + ":redis");

        return (Cache<K, V>) this.caches.computeIfAbsent(name + ":two-level",
                k -> new MultiLevelCache<K, V>(name, Arrays.asList(localCache, redisCache), writeThrough, asyncWrite,
                        syncLocal));
    }

    /**
     * 创建或获取可自动刷新的缓存
     * 
     * @param name            缓存名称
     * @param refreshInterval 刷新间隔
     * @param refreshTimeUnit 刷新间隔时间单位
     * @param threadPoolSize  线程池大小
     * @return 可自动刷新的缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> RefreshableCache<K, V> getOrCreateRefreshableCache(String name, long refreshInterval,
            TimeUnit refreshTimeUnit, int threadPoolSize) {

        Cache<K, V> baseCache = getOrCreateLocalCache(name + ":base");

        return (RefreshableCache<K, V>) this.caches.computeIfAbsent(name + ":refreshable",
                k -> new RefreshableCache<K, V>(baseCache, refreshInterval, refreshTimeUnit, threadPoolSize));
    }

    /**
     * 创建或获取可自动刷新的Redis缓存
     * 
     * @param name            缓存名称
     * @param refreshInterval 刷新间隔
     * @param refreshTimeUnit 刷新间隔时间单位
     * @param threadPoolSize  线程池大小
     * @return 可自动刷新的Redis缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> RefreshableCache<K, V> getOrCreateRefreshableRedisCache(String name, long refreshInterval,
            TimeUnit refreshTimeUnit, int threadPoolSize) {

        if (jedisPool == null) {
            throw new IllegalStateException("JedisPool is not set");
        }
        if (serializer == null) {
            throw new IllegalStateException("Serializer is not set");
        }

        Cache<K, V> baseCache = getOrCreateRedisCache(name + ":base");

        return (RefreshableCache<K, V>) this.caches.computeIfAbsent(name + ":refreshable-redis",
                k -> new RefreshableCache<K, V>(baseCache, refreshInterval, refreshTimeUnit, threadPoolSize));
    }

    /**
     * 创建或获取可自动刷新的二级缓存（本地缓存 + Redis缓存）
     * 
     * @param name            缓存名称
     * @param refreshInterval 刷新间隔
     * @param refreshTimeUnit 刷新间隔时间单位
     * @param threadPoolSize  刷新线程池大小
     * @param writeThrough    是否写透
     * @param asyncWrite      是否异步写入
     * @param syncLocal       是否同步本地缓存
     * @return 可自动刷新的多级缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> RefreshableCache<K, V> getOrCreateRefreshableTwoLevelCache(String name, long refreshInterval,
            TimeUnit refreshTimeUnit, int threadPoolSize, boolean writeThrough, boolean asyncWrite, boolean syncLocal) {

        Cache<K, V> baseCache = getOrCreateTwoLevelCache(name + ":base", writeThrough, asyncWrite, syncLocal);

        return (RefreshableCache<K, V>) this.caches.computeIfAbsent(name + ":refreshable-two-level",
                k -> new RefreshableCache<K, V>(baseCache, refreshInterval, refreshTimeUnit, threadPoolSize));
    }

    /**
     * 获取缓存
     * 
     * @param name 缓存名称
     * @return 缓存实例，如果不存在则返回null
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
        return cache != null;
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        caches.values().forEach(Cache::clear);
    }

    /**
     * 根据快速配置创建或获取缓存
     * 
     * @param config 快速配置
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(QuickConfig config) {
        String name = config.getName();
        QuickConfig.CacheType cacheType = config.getCacheType();

        // 根据缓存类型创建缓存
        switch (cacheType) {
            case REMOTE:
                return getOrCreateRedisCache(name);
            case BOTH:
                return getOrCreateTwoLevelCache(name, config.isWriteThrough(), config.isAsyncWrite(),
                        config.isSyncLocal());
            case LOCAL:
            default:
                Cache<K, V> localCache = getOrCreateLocalCache(name);

                // 如果需要自动刷新，创建可刷新缓存
                if (config.isRefreshable()) {
                    return (Cache<K, V>) caches.computeIfAbsent(name + ":refreshable",
                            k -> new RefreshableCache<K, V>(localCache,
                                    config.getRefreshInterval(),
                                    config.getRefreshTimeUnit(),
                                    2));
                }

                return localCache;
        }
    }

    /**
     * 设置本地缓存最大大小
     */
    public void setLocalCacheMaximumSize(int maximumSize) {
        // 这里可以设置本地缓存的最大大小
    }

    /**
     * 设置本地缓存初始容量
     */
    public void setLocalCacheInitialCapacity(int initialCapacity) {
        // 这里可以设置本地缓存的初始容量
    }

    /**
     * 设置默认本地缓存过期时间
     */
    public void setDefaultLocalExpiration(long expireTime) {
        // 这里可以设置默认本地缓存过期时间
    }

    /**
     * 设置默认本地缓存过期时间单位
     */
    public void setDefaultLocalTimeUnit(TimeUnit timeUnit) {
        // 这里可以设置默认本地缓存过期时间单位
    }

    /**
     * 设置默认Redis缓存过期时间
     */
    public void setDefaultRedisExpiration(long expireTime) {
        // 这里可以设置默认Redis缓存过期时间
    }

    /**
     * 设置默认Redis缓存过期时间单位
     */
    public void setDefaultRedisTimeUnit(TimeUnit timeUnit) {
        // 这里可以设置默认Redis缓存过期时间单位
    }

    /**
     * 设置写透模式
     */
    public void setWriteThrough(boolean writeThrough) {
        // 这里可以设置写透模式
    }

    /**
     * 设置异步写入
     */
    public void setAsyncWrite(boolean asyncWrite) {
        // 这里可以设置异步写入
    }

    /**
     * 初始化缓存同步
     */
    public void initCacheSync() {
        if (jedisPool == null) {
            throw new IllegalStateException("JedisPool is not set");
        }
        if (serializer == null) {
            throw new IllegalStateException("Serializer is not set");
        }

        CacheSyncManager.getInstance().init(jedisPool, serializer);
    }

    /**
     * 关闭缓存管理器
     */
    public void shutdown() {
        // 关闭缓存同步管理器
        CacheSyncManager.getInstance().shutdown();

        // 清空所有缓存
        clearAll();
    }
}