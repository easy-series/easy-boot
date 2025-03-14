package com.easy.cache.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 缓存同步管理器，负责缓存同步策略
 */
public class CacheSyncManager {

    /**
     * 事件发布器
     */
    private final CacheEventPublisher publisher;
    
    /**
     * 事件订阅器
     */
    private final CacheEventSubscriber subscriber;
    
    /**
     * 事件监听器列表
     */
    private final List<CacheEventListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * 是否启用同步
     */
    private boolean enabled = false;
    
    /**
     * 构造函数
     *
     * @param publisher 事件发布器
     * @param subscriber 事件订阅器
     */
    public CacheSyncManager(CacheEventPublisher publisher, CacheEventSubscriber subscriber) {
        this.publisher = publisher;
        this.subscriber = subscriber;
    }
    
    /**
     * 启用缓存同步
     */
    public void enable() {
        if (!enabled) {
            subscriber.start();
            
            // 注册所有监听器
            for (CacheEventListener listener : listeners) {
                subscriber.subscribe(listener);
            }
            
            enabled = true;
        }
    }
    
    /**
     * 禁用缓存同步
     */
    public void disable() {
        if (enabled) {
            // 取消注册所有监听器
            for (CacheEventListener listener : listeners) {
                subscriber.unsubscribe(listener);
            }
            
            subscriber.stop();
            enabled = false;
        }
    }
    
    /**
     * 发布缓存事件
     *
     * @param event 缓存事件
     */
    public void publishEvent(CacheEvent event) {
        if (enabled) {
            publisher.publish(event);
        }
    }
    
    /**
     * 添加事件监听器
     *
     * @param listener 事件监听器
     */
    public void addListener(CacheEventListener listener) {
        listeners.add(listener);
        if (enabled) {
            subscriber.subscribe(listener);
        }
    }
    
    /**
     * 移除事件监听器
     *
     * @param listener 事件监听器
     */
    public void removeListener(CacheEventListener listener) {
        listeners.remove(listener);
        if (enabled) {
            subscriber.unsubscribe(listener);
        }
    }
    
    /**
     * 获取所有监听器
     *
     * @return 监听器列表
     */
    public List<CacheEventListener> getListeners() {
        return new ArrayList<>(listeners);
    }
    
    /**
     * 是否已启用
     *
     * @return 是否已启用
     */
    public boolean isEnabled() {
        return enabled;
    }
} 