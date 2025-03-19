package com.easy.cache.core;

import java.util.Set;

/**
 * 缓存管理器接口
 */
public interface CacheManager {
    /**
     * 获取缓存
     */
    <K, V> Cache<K, V> getCache(String name);

    /**
     * 获取缓存，使用指定的配置
     */
    <K, V> Cache<K, V> getCache(String name, CacheConfig config);

    /**
     * 获取所有缓存名称
     */
    Set<String> getCacheNames();

    /**
     * 移除缓存
     */
    void removeCache(String name);

    /**
     * 销毁缓存
     */
    void destroyCache(String name);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 获取缓存配置
     */
    CacheConfig getConfig();

    /**
     * 设置缓存配置
     */
    void setConfig(CacheConfig config);
}