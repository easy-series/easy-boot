package com.easy.cache.support.sync;

/**
 * 缓存事件订阅器接口，用于订阅缓存事件
 */
public interface CacheEventSubscriber {
    
    /**
     * 订阅缓存事件
     */
    void subscribe();
    
    /**
     * 取消订阅
     */
    void unsubscribe();
}