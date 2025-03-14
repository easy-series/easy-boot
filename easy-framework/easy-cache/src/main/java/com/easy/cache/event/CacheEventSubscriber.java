package com.easy.cache.event;

/**
 * 缓存事件订阅器接口
 */
public interface CacheEventSubscriber {
    
    /**
     * 订阅缓存事件
     *
     * @param listener 事件监听器
     */
    void subscribe(CacheEventListener listener);
    
    /**
     * 取消订阅缓存事件
     *
     * @param listener 事件监听器
     */
    void unsubscribe(CacheEventListener listener);
    
    /**
     * 启动订阅器
     */
    void start();
    
    /**
     * 停止订阅器
     */
    void stop();
} 