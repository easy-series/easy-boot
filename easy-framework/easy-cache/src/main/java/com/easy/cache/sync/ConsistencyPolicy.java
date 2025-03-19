package com.easy.cache.sync;

import com.easy.cache.core.MultiLevelCache;

/**
 * 一致性策略接口
 */
public interface ConsistencyPolicy {
    /**
     * 处理写入操作
     *
     * @param cache 多级缓存
     * @param key   缓存键
     * @param value 缓存值
     */
    void handleWrite(MultiLevelCache<?, ?> cache, Object key, Object value);

    /**
     * 处理删除操作
     *
     * @param cache 多级缓存
     * @param key   缓存键
     */
    void handleRemove(MultiLevelCache<?, ?> cache, Object key);

    /**
     * 处理清空操作
     *
     * @param cache 多级缓存
     */
    void handleClear(MultiLevelCache<?, ?> cache);
}