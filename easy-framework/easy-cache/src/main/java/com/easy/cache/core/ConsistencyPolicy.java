package com.easy.cache.core;

/**
 * 缓存一致性策略接口
 */
public interface ConsistencyPolicy {

    /**
     * 写入缓存
     */
    void write(String cacheName, Object key, Object value);

    /**
     * 删除缓存
     */
    void delete(String cacheName, Object key);

    /**
     * 清空缓存
     */
    void clear(String cacheName);
}