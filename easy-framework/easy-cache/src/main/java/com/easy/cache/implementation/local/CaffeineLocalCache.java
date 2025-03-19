package com.easy.cache.implementation.local;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.monitor.CacheStats;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

/**
 * 基于Caffeine的本地缓存实现
 */
public class CaffeineLocalCache<K, V> implements Cache<K, V> {
    private final String name;
    private final CacheConfig config;
    private final LoadingCache<K, V> cache;

    public CaffeineLocalCache(String name, CacheConfig config) {
        this.name = name;
        this.config = config;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(config.getLocalExpireSeconds(), TimeUnit.SECONDS)
                .maximumSize(config.getLocalMaxSize())
                .build(key -> null);
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void put(K key, V value, long expireSeconds) {
        cache.put(key, value);
    }

    @Override
    public void remove(K key) {
        cache.invalidate(key);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
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
        return cache.getIfPresent(key) != null;
    }

    @Override
    public CacheStats stats() {
        return null;
    }
} 