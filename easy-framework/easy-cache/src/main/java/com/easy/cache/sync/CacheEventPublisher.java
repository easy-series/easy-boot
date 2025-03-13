package com.easy.cache.sync;

/**
 * 缓存事件发布者接口
 */
public interface CacheEventPublisher {

    /**
     * 发布缓存事件
     * 
     * @param event 缓存事件
     */
    void publish(CacheEvent event);

    /**
     * 关闭发布者
     */
    void shutdown();
}