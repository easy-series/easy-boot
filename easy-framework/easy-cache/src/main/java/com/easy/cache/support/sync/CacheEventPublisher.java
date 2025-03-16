package com.easy.cache.support.sync;

/**
 * 缓存事件发布器接口，用于发布缓存事件
 */
public interface CacheEventPublisher {
    
    /**
     * 发布缓存事件
     *
     * @param event 缓存事件
     */
    void publish(CacheEvent event);
} 