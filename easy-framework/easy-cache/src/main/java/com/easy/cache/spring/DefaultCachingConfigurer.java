package com.easy.cache.spring;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheManager;
import com.easy.cache.util.SpELKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 默认缓存配置器
 */
public class DefaultCachingConfigurer implements CachingConfigurer {

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Override
    public CacheManager cacheManager() {
        return cacheManager;
    }

    @Override
    public SpELKeyGenerator keyGenerator() {
        return new SpELKeyGenerator();
    }

    @Override
    public <K, V> Cache<K, V> createCache(String name, CacheConfig config) {
        return cacheManager.getCache(name, config);
    }
} 