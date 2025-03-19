package com.easy.cache.implementation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheManager;
import com.easy.cache.implementation.local.CaffeineLocalCache;
import com.easy.cache.implementation.remote.RedisRemoteCache;
import com.easy.cache.sync.CacheEventPublisher;
import com.easy.cache.sync.CacheEventSubscriber;

/**
 * 默认缓存管理器实现
 */
@Component
public class DefaultCacheManager implements CacheManager {

    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private CacheEventPublisher eventPublisher;

    @Autowired(required = false)
    private CacheEventSubscriber eventSubscriber;

    private CacheConfig config = CacheConfig.builder().build();

    @Override
    public <K, V> Cache<K, V> getCache(String name) {
        return getCache(name, CacheConfig.builder().build());
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name, CacheConfig config) {
        return (Cache<K, V>) caches.computeIfAbsent(name, k -> createCache(name, config));
    }

    @Override
    public Set<String> getCacheNames() {
        return caches.keySet();
    }

    @Override
    public void destroyCache(String name) {
        Cache<?, ?> cache = caches.remove(name);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public void removeCache(String name) {
        destroyCache(name);
    }

    @Override
    public void clear() {
        caches.values().forEach(Cache::clear);
        caches.clear();
    }

    @Override
    public CacheConfig getConfig() {
        return config;
    }

    @Override
    public void setConfig(CacheConfig config) {
        this.config = config;
    }

    private <K, V> Cache<K, V> createCache(String name, CacheConfig config) {
        // 根据配置创建本地缓存或远程缓存
        if (config.isLocal()) {
            return new CaffeineLocalCache<>(name, config);
        } else {
            return new RedisRemoteCache<>(name, config, null, null, eventPublisher, eventSubscriber);
        }
    }
}