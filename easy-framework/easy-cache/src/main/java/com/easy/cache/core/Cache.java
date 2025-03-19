package com.easy.cache.core;

import com.easy.cache.monitor.CacheStats;

/**
 * 缓存接口
 */
public interface Cache<K, V> {

    /**
     * 获取缓存值
     */
    V get(K key);

    /**
     * 设置缓存值
     */
    void put(K key, V value);

    /**
     * 设置缓存值，并指定过期时间
     */
    void put(K key, V value, long expireSeconds);

    /**
     * 删除缓存值
     */
    void remove(K key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 获取缓存名称
     */
    String getName();

    /**
     * 获取缓存配置
     */
    CacheConfig getConfig();

    /**
     * 检查键是否存在
     */
    boolean containsKey(K key);

    /**
     * 获取缓存统计信息
     */
    CacheStats stats();
}