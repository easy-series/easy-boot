package com.easy.cache.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheManager;
import com.easy.cache.service.CacheService;

/**
 * 默认缓存服务实现
 */
@Service
public class DefaultCacheService implements CacheService {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public <T> T get(String cacheName, Object key) {
        Cache<Object, T> cache = cacheManager.getCache(cacheName);
        return cache != null ? cache.get(key) : null;
    }

    @Override
    public <T> void put(String cacheName, Object key, T value) {
        Cache<Object, T> cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    @Override
    public void remove(String cacheName, Object key) {
        Cache<Object, ?> cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.remove(key);
        }
    }

    @Override
    public void clear(String cacheName) {
        Cache<Object, ?> cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public boolean contains(String cacheName, Object key) {
        Cache<Object, ?> cache = cacheManager.getCache(cacheName);
        return cache != null && cache.containsKey(key);
    }

    @Override
    public CacheConfig getConfig(String cacheName) {
        Cache<Object, ?> cache = cacheManager.getCache(cacheName);
        return cache != null ? cache.getConfig() : null;
    }

    @Override
    public void createCache(String cacheName, CacheConfig config) {
        cacheManager.getCache(cacheName, config);
    }
}