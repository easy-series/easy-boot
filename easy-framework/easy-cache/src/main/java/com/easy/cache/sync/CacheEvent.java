package com.easy.cache.sync;

import java.io.Serializable;

/**
 * 缓存事件，用于在不同JVM实例之间同步缓存操作
 */
public class CacheEvent implements Serializable {
    private static final long serialVersionUID = 1L;

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
     * 事件类型
     */
    private EventType eventType;

    /**
     * 事件发生时间戳
     */
    private long timestamp;

    /**
     * 创建一个缓存事件
     * 
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param eventType 事件类型
     */
    public CacheEvent(String cacheName, Object key, Object value, EventType eventType) {
        this.cacheName = cacheName;
        this.key = key;
        this.value = value;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建一个清空缓存事件
     * 
     * @param cacheName 缓存名称
     * @return 清空缓存事件
     */
    public static CacheEvent createClearEvent(String cacheName) {
        return new CacheEvent(cacheName, null, null, EventType.CLEAR);
    }

    /**
     * 创建一个删除缓存事件
     * 
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @return 删除缓存事件
     */
    public static CacheEvent createRemoveEvent(String cacheName, Object key) {
        return new CacheEvent(cacheName, key, null, EventType.REMOVE);
    }

    /**
     * 创建一个添加或更新缓存事件
     * 
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @return 添加或更新缓存事件
     */
    public static CacheEvent createPutEvent(String cacheName, Object key, Object value) {
        return new CacheEvent(cacheName, key, value, EventType.PUT);
    }

    public String getCacheName() {
        return cacheName;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public EventType getEventType() {
        return eventType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "CacheEvent [cacheName=" + cacheName + ", key=" + key + ", eventType=" + eventType + ", timestamp="
                + timestamp + "]";
    }
}