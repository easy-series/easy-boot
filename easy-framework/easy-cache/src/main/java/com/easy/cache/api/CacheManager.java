package com.easy.cache.api;

import com.easy.cache.config.CacheConfig;
import com.easy.cache.config.QuickConfig;

/**
 * 缓存管理器接口，用于管理缓存实例
 */
public interface CacheManager {

    /**
     * 获取指定名称的缓存
     *
     * @param name 缓存名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 缓存实例，不存在则返回null
     */
    <K, V> Cache<K, V> getCache(String name);

    /**
     * 获取或创建缓存，如果不存在则根据配置创建
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 缓存实例
     */
    <K, V> Cache<K, V> getOrCreateCache(CacheConfig<K, V> config);

    /**
     * 获取或创建缓存，如果不存在则根据快速配置创建
     *
     * @param quickConfig 快速缓存配置
     * @param <K>         键类型
     * @param <V>         值类型
     * @return 缓存实例
     */
    <K, V> Cache<K, V> getOrCreateCache(QuickConfig quickConfig);

    /**
     * 移除指定名称的缓存
     *
     * @param name 缓存名称
     */
    void removeCache(String name);

    /**
     * 清空所有缓存
     */
    void clear();
}