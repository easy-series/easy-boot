package com.easy.cache.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存构建器，用于创建缓存实例
 */
public class CacheBuilder<K, V> {

    private final CacheConfig config = new CacheConfig();
    private boolean useRedis = false;
    private boolean useMultiLevel = false;
    private boolean useTwoLevel = false;
    private boolean writeThrough = true;
    private boolean asyncWrite = false;
    private final List<Cache<K, V>> caches = new ArrayList<>();

    // 自动刷新相关配置
    private boolean refreshable = false;
    private long refreshInterval = 5;
    private TimeUnit refreshTimeUnit = TimeUnit.MINUTES;
    private int refreshThreadPoolSize = 2;

    private CacheBuilder() {
    }

    /**
     * 创建缓存构建器
     */
    public static <K, V> CacheBuilder<K, V> newBuilder() {
        return new CacheBuilder<>();
    }

    /**
     * 设置缓存名称
     */
    public CacheBuilder<K, V> name(String name) {
        config.setName(name);
        return this;
    }

    /**
     * 设置过期时间
     */
    public CacheBuilder<K, V> expireAfterWrite(long expireTime, TimeUnit timeUnit) {
        config.setExpireTime(expireTime);
        config.setTimeUnit(timeUnit);
        return this;
    }

    /**
     * 设置是否允许缓存null值
     */
    public CacheBuilder<K, V> allowNullValues(boolean allowNullValues) {
        config.setAllowNullValues(allowNullValues);
        return this;
    }

    /**
     * 设置使用Redis缓存
     */
    public CacheBuilder<K, V> useRedis() {
        this.useRedis = true;
        return this;
    }

    /**
     * 设置使用二级缓存（本地缓存 + Redis缓存）
     */
    public CacheBuilder<K, V> useTwoLevel() {
        this.useTwoLevel = true;
        return this;
    }

    /**
     * 设置使用多级缓存
     */
    public CacheBuilder<K, V> useMultiLevel() {
        this.useMultiLevel = true;
        return this;
    }

    /**
     * 添加缓存层
     */
    public CacheBuilder<K, V> addCache(Cache<K, V> cache) {
        if (cache != null) {
            this.caches.add(cache);
        }
        return this;
    }

    /**
     * 设置是否写透（写入时是否同时写入所有缓存层）
     */
    public CacheBuilder<K, V> writeThrough(boolean writeThrough) {
        this.writeThrough = writeThrough;
        return this;
    }

    /**
     * 设置是否异步写入低优先级缓存
     */
    public CacheBuilder<K, V> asyncWrite(boolean asyncWrite) {
        this.asyncWrite = asyncWrite;
        return this;
    }

    /**
     * 设置缓存为可自动刷新
     */
    public CacheBuilder<K, V> refreshable() {
        this.refreshable = true;
        return this;
    }

    /**
     * 设置缓存刷新间隔
     */
    public CacheBuilder<K, V> refreshInterval(long refreshInterval, TimeUnit refreshTimeUnit) {
        this.refreshInterval = refreshInterval;
        this.refreshTimeUnit = refreshTimeUnit;
        return this;
    }

    /**
     * 设置刷新线程池大小
     */
    public CacheBuilder<K, V> refreshThreadPoolSize(int threadPoolSize) {
        this.refreshThreadPoolSize = threadPoolSize;
        return this;
    }

    /**
     * 构建本地缓存
     */
    public Cache<K, V> buildLocalCache() {
        if (config.getName() == null || config.getName().isEmpty()) {
            throw new IllegalArgumentException("Cache name cannot be null or empty");
        }

        Cache<K, V> cache = CacheManager.getInstance().getOrCreateLocalCache(config.getName());

        if (refreshable) {
            return CacheManager.getInstance().getOrCreateRefreshableCache(
                    config.getName(), refreshInterval, refreshTimeUnit, refreshThreadPoolSize);
        }

        return cache;
    }

    /**
     * 构建Redis缓存
     */
    public Cache<K, V> buildRedisCache() {
        if (config.getName() == null || config.getName().isEmpty()) {
            throw new IllegalArgumentException("Cache name cannot be null or empty");
        }

        Cache<K, V> cache = CacheManager.getInstance().getOrCreateRedisCache(config.getName());

        if (refreshable) {
            return CacheManager.getInstance().getOrCreateRefreshableRedisCache(
                    config.getName(), refreshInterval, refreshTimeUnit, refreshThreadPoolSize);
        }

        return cache;
    }

    /**
     * 构建二级缓存（本地缓存 + Redis缓存）
     */
    public Cache<K, V> buildTwoLevelCache() {
        if (config.getName() == null || config.getName().isEmpty()) {
            throw new IllegalArgumentException("Cache name cannot be null or empty");
        }

        Cache<K, V> cache = CacheManager.getInstance().getOrCreateTwoLevelCache(config.getName(), writeThrough,
                asyncWrite, config.isSyncLocal());

        if (refreshable) {
            return CacheManager.getInstance().getOrCreateRefreshableTwoLevelCache(
                    config.getName(), refreshInterval, refreshTimeUnit, refreshThreadPoolSize, writeThrough,
                    asyncWrite, config.isSyncLocal());
        }

        return cache;
    }

    /**
     * 构建多级缓存
     */
    @SuppressWarnings("unchecked")
    public Cache<K, V> buildMultiLevelCache() {
        if (caches.isEmpty()) {
            throw new IllegalArgumentException("No cache levels added");
        }

        @SuppressWarnings("unchecked")
        Cache<K, V> cache = CacheManager.getInstance().getOrCreateMultiLevelCache(
                config.getName(), writeThrough, asyncWrite, config.isSyncLocal(), caches.toArray(new Cache[0]));

        if (refreshable) {
            return new RefreshableCache<>(cache, refreshInterval, refreshTimeUnit, refreshThreadPoolSize);
        }

        return cache;
    }

    /**
     * 构建缓存
     */
    public Cache<K, V> build() {
        if (useTwoLevel) {
            return buildTwoLevelCache();
        } else if (useMultiLevel) {
            return buildMultiLevelCache();
        } else if (useRedis) {
            return buildRedisCache();
        } else {
            return buildLocalCache();
        }
    }
}