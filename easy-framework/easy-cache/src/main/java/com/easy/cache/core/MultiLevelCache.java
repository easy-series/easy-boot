package com.easy.cache.core;

import com.easy.cache.sync.ConsistencyPolicy;

/**
 * 多级缓存接口
 */
public interface MultiLevelCache<K, V> extends Cache<K, V> {
    /**
     * 获取本地缓存
     *
     * @return 本地缓存
     */
    Cache<K, V> getLocalCache();

    /**
     * 获取远程缓存
     *
     * @return 远程缓存
     */
    Cache<K, V> getRemoteCache();

    /**
     * 获取一致性策略
     *
     * @return 一致性策略
     */
    ConsistencyPolicy getConsistencyPolicy();
}