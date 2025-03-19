package com.easy.cache.spring;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheManager;
import com.easy.cache.util.SpELKeyGenerator;

/**
 * 缓存配置器接口
 */
public interface CachingConfigurer {
    /**
     * 获取缓存管理器
     *
     * @return 缓存管理器
     */
    CacheManager cacheManager();

    /**
     * 获取键生成器
     *
     * @return 键生成器
     */
    SpELKeyGenerator keyGenerator();

    /**
     * 创建缓存
     *
     * @param name   缓存名称
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 缓存实例
     */
    <K, V> Cache<K, V> createCache(String name, CacheConfig config);
}