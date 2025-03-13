package com.easy.cache.sync;

/**
 * 缓存事件订阅者接口
 */
public interface CacheEventSubscriber {

    /**
     * 订阅缓存事件
     * 
     * @param listener 事件监听器
     */
    void subscribe(CacheEventListener listener);

    /**
     * 取消订阅
     * 
     * @param listener 事件监听器
     */
    void unsubscribe(CacheEventListener listener);

    /**
     * 启动订阅者
     */
    void start();

    /**
     * 关闭订阅者
     */
    void shutdown();
}