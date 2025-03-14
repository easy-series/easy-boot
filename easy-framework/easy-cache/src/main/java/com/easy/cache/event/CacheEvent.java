package com.easy.cache.event;

import java.io.Serializable;

/**
 * 缓存事件类，表示缓存的变更操作
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
    private String key;

    /**
     * 缓存值，仅用于PUT事件
     */
    private Object value;

    /**
     * 事件ID，用于去重
     */
    private String eventId;

    /**
     * 事件发生时间戳
     */
    private long timestamp;

    /**
     * 构造函数
     */
    public CacheEvent() {
        this.timestamp = System.currentTimeMillis();
        this.eventId = java.util.UUID.randomUUID().toString();
    }

    /**
     * 创建PUT事件
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param value 缓存值
     * @return 缓存事件
     */
    public static CacheEvent createPutEvent(String cacheName, String key, Object value) {
        CacheEvent event = new CacheEvent();
        event.setEventType(EventType.PUT);
        event.setCacheName(cacheName);
        event.setKey(key);
        event.setValue(value);
        return event;
    }

    /**
     * 创建REMOVE事件
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @return 缓存事件
     */
    public static CacheEvent createRemoveEvent(String cacheName, String key) {
        CacheEvent event = new CacheEvent();
        event.setEventType(EventType.REMOVE);
        event.setCacheName(cacheName);
        event.setKey(key);
        return event;
    }

    /**
     * 创建CLEAR事件
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

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 放入缓存
         */
        PUT,
        
        /**
         * 移除缓存
         */
        REMOVE,
        
        /**
         * 清空缓存
         */
        CLEAR
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "CacheEvent{" +
                "eventType=" + eventType +
                ", cacheName='" + cacheName + '\'' +
                ", key='" + key + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 