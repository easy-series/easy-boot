package com.easy.cache.implementation;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.monitor.CacheStats;
import com.easy.cache.sync.lock.DistributedLock;

/**
 * 简单多级缓存实现
 * 采用"先写本地，后写远程"的策略
 */
public class SimpleMultiLevelCache<K, V> implements Cache<K, V> {
    private final String name;
    private final CacheConfig config;
    private final Cache<K, V> localCache;
    private final Cache<K, V> remoteCache;
    private final DistributedLock distributedLock;

    public SimpleMultiLevelCache(String name, CacheConfig config, Cache<K, V> localCache, Cache<K, V> remoteCache, DistributedLock distributedLock) {
        this.name = localCache.getName();
        this.config = localCache.getConfig();
        this.localCache = localCache;
        this.remoteCache = remoteCache;
        this.distributedLock = distributedLock;
    }

    @Override
    public V get(K key) {
        // 先从本地缓存获取
        V value = localCache.get(key);
        if (value != null) {
            return value;
        }

        // 本地缓存未命中，尝试从远程缓存获取
        String lockKey = buildLockKey(key);
        if (distributedLock.tryLock(lockKey)) {
            try {
                // 双重检查
                value = localCache.get(key);
                if (value != null) {
                    return value;
                }

                value = remoteCache.get(key);
                if (value != null) {
                    // 将远程缓存的值同步到本地缓存
                    localCache.put(key, value);
                }
                return value;
            } finally {
                distributedLock.unlock(lockKey);
            }
        }

        return null;
    }

    @Override
    public void put(K key, V value) {
        // 先写入本地缓存
        localCache.put(key, value);

        // 再写入远程缓存
        remoteCache.put(key, value);
    }

    @Override
    public void put(K key, V value, long expireSeconds) {
        // 先写入本地缓存
        localCache.put(key, value, expireSeconds);

        // 再写入远程缓存
        remoteCache.put(key, value, expireSeconds);
    }

    @Override
    public void remove(K key) {
        // 先删除本地缓存
        localCache.remove(key);

        // 再删除远程缓存
        remoteCache.remove(key);
    }

    @Override
    public void clear() {
        // 先清空本地缓存
        localCache.clear();

        // 再清空远程缓存
        remoteCache.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CacheConfig getConfig() {
        return config;
    }

    @Override
    public boolean containsKey(K key) {
        return localCache.containsKey(key) || remoteCache.containsKey(key);
    }

    @Override
    public CacheStats stats() {
        return null;
    }


    private String buildLockKey(K key) {
        return name + ":lock:" + key;
    }
}