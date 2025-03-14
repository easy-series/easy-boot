package com.easy.cache.sync;

import java.io.Serializable;

/**
 * 缓存事件，用于缓存同步
 */
public class CacheEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存键
     */
    private Object key;

    /**
     * 缓存值
     */
    private Object value;

    /**
     * 创建一个PUT事件
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @return 缓存事件
     */
    public static CacheEvent createPutEvent(String cacheName, Object key, Object value) {
        CacheEvent event = new CacheEvent();
        event.setEventType(EventType.PUT);
        event.setCacheName(cacheName);
        event.setKey(key);
        event.setValue(value);
        return event;
    }

    /**
     * 创建一个REMOVE事件
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @return 缓存事件
     */
    public static CacheEvent createRemoveEvent(String cacheName, Object key) {
        CacheEvent event = new CacheEvent();
        event.setEventType(EventType.REMOVE);
        event.setCacheName(cacheName);
        event.setKey(key);
        return event;
    }

    /**
     * 创建一个CLEAR事件
     *
     * @param cacheName 缓存名称
     * @return 缓存事件
     */
    public static CacheEvent createClearEvent(String cacheName) {
        CacheEvent event = new CacheEvent();
        event.setEventType(EventType.CLEAR);
        event.setCacheName(cacheName);
        return event;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "CacheEvent{" +
                "eventType=" + eventType +
                ", cacheName='" + cacheName + '\'' +
                ", key=" + key +
                '}';
    }

    /**
     * 缓存事件类型
     */
    public enum EventType {
        /**
         * 添加或更新缓存
         */
        PUT,

        /**
         * 删除缓存
         */
        REMOVE,

        /**
         * 清空缓存
         */
        CLEAR
    }
}