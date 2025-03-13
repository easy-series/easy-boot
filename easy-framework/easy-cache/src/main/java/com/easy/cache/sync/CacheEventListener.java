package com.easy.cache.sync;

/**
 * 缓存事件监听器接口
 */
public interface CacheEventListener {

    /**
     * 处理缓存事件
     * 
     * @param event 缓存事件
     */
    void onEvent(CacheEvent event);
}