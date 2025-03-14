package com.easy.cache.event;

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
    
    /**
     * 是否处理该缓存事件
     *
     * @param event 缓存事件
     * @return 是否处理
     */
    default boolean supports(CacheEvent event) {
        return true;
    }
} 