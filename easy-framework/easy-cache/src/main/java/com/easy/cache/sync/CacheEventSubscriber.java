package com.easy.cache.sync;

/**
 * 缓存事件订阅器接口
 */
public interface CacheEventSubscriber {
    /**
     * 处理缓存事件
     *
     * @param event 缓存事件
     */
    void onMessage(CacheEvent event);
}