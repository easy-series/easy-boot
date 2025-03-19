package com.easy.cache.core;

import java.util.Map;

/**
 * 缓存加载器接口
 */
public interface CacheLoader<K, V> {
    /**
     * 加载缓存值
     */
    V load(K key);

    /**
     * 批量加载缓存值
     */
    Map<K, V> loadAll(Iterable<? extends K> keys);
}