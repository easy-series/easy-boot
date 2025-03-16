package com.easy.cache.support.sync;

import com.easy.cache.core.embedded.LocalCache;

/**
 * 本地缓存同步管理器接口
 */
public interface LocalCacheSyncManager {

    /**
     * 注册本地缓存
     *
     * @param cacheName 缓存名称
     * @param cache     本地缓存实例
     */
    void registerLocalCache(String cacheName, LocalCache<?, ?> cache);

    /**
     * 注销本地缓存
     *
     * @param cacheName 缓存名称
     */
    void unregisterLocalCache(String cacheName);

    /**
     * 处理缓存事件
     *
     * @param event 缓存事件
     */
    void handleCacheEvent(CacheEvent event);
}