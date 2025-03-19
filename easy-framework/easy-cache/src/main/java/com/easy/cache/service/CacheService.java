package com.easy.cache.service;

import com.easy.cache.core.CacheConfig;

/**
 * 缓存服务接口
 * 提供统一的缓存操作能力
 */
public interface CacheService {

    /**
     * 从缓存中获取值
     *
     * @param cacheName 缓存名称
     * @param key       键
     * @param <T>       值类型
     * @return 缓存的值，如果不存在则返回null
     */
    <T> T get(String cacheName, Object key);

    /**
     * 将值放入缓存
     *
     * @param cacheName 缓存名称
     * @param key       键
     * @param value     值
     * @param <T>       值类型
     */
    <T> void put(String cacheName, Object key, T value);

    /**
     * 从缓存中删除指定的键
     *
     * @param cacheName 缓存名称
     * @param key       键
     */
    void remove(String cacheName, Object key);

    /**
     * 清空指定缓存
     *
     * @param cacheName 缓存名称
     */
    void clear(String cacheName);

    /**
     * 检查缓存中是否存在指定的键
     *
     * @param cacheName 缓存名称
     * @param key       键
     * @return 如果存在则返回true，否则返回false
     */
    boolean contains(String cacheName, Object key);

    /**
     * 获取缓存配置
     *
     * @param cacheName 缓存名称
     * @return 缓存配置
     */
    CacheConfig getConfig(String cacheName);

    /**
     * 创建指定名称的缓存
     *
     * @param cacheName 缓存名称
     * @param config    缓存配置
     */
    void createCache(String cacheName, CacheConfig config);
}