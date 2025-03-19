package com.easy.cache.builder;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.MultiLevelCache;
import com.easy.cache.implementation.DefaultCacheManager;
import com.easy.cache.implementation.SimpleMultiLevelCache;
import com.easy.cache.implementation.local.CaffeineLocalCache;
import com.easy.cache.implementation.remote.RedisRemoteCache;
import com.easy.cache.sync.lock.DefaultDistributedLock;
import com.easy.cache.sync.lock.DistributedLock;

/**
 * 缓存构建器
 * 提供流式API创建各种缓存
 */
public class CacheBuilder {
    private Cache<?, ?> localCache;
    private Cache<?, ?> remoteCache;
    private CacheConfig config;
    private DistributedLock distributedLock;

    private CacheBuilder() {
        this.distributedLock = new DefaultDistributedLock();
    }

    /**
     * 创建构建器实例
     */
    public static CacheBuilder builder() {
        return new CacheBuilder();
    }

    /**
     * 设置本地缓存
     */
    public CacheBuilder withLocalCache(Cache<?, ?> localCache) {
        this.localCache = localCache;
        return this;
    }

    /**
     * 设置远程缓存
     */
    public CacheBuilder withRemoteCache(Cache<?, ?> remoteCache) {
        this.remoteCache = remoteCache;
        return this;
    }

    /**
     * 设置缓存配置
     */
    public CacheBuilder withConfig(CacheConfig config) {
        this.config = config;
        return this;
    }

    /**
     * 设置分布式锁
     */
    public CacheBuilder withDistributedLock(DistributedLock distributedLock) {
        this.distributedLock = distributedLock;
        return this;
    }

    /**
     * 构建缓存管理器
     */
    public CacheManager build() {
        if (config == null) {
            config = CacheConfig.builder().build();
        }

        DefaultCacheManager cacheManager = new DefaultCacheManager();
        cacheManager.setConfig(config);
        
        // 注意：这里我们只设置了配置，没有直接注册缓存
        // DefaultCacheManager会在调用getCache时懒加载缓存
        // 如果有需要可以在这里预热一些常用缓存
        
        return cacheManager;
    }

    /**
     * 构建简单多级缓存
     */
    public <K, V> MultiLevelCache<K, V> buildMultiLevelCache(String name) {
        if (config == null) {
            config = CacheConfig.builder().build();
        }

        // 如果未提供本地缓存，则创建默认本地缓存
        if (localCache == null) {
            localCache = new CaffeineLocalCache<>(name + "_local", config);
        }

        // 如果未提供远程缓存，则创建默认远程缓存
        if (remoteCache == null) {
            remoteCache = new RedisRemoteCache<>(name + "_remote", config, null, null, null, null);
        }

        return (MultiLevelCache<K, V>) new SimpleMultiLevelCache<>(
                name,
                config,
                (Cache<K, V>) localCache,
                (Cache<K, V>) remoteCache,
                distributedLock);
    }
}