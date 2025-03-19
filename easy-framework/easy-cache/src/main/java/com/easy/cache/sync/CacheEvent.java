package com.easy.cache.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 缓存事件
 */
public class CacheEvent {

    private String cacheName;
    private Object key;
    private Object value;
    private EventType eventType;

    /**
     * 默认构造函数，用于Jackson反序列化
     */
    public CacheEvent() {
        // 默认构造函数，用于反序列化
    }

    /**
     * 全参数构造函数
     */
    @JsonCreator
    public CacheEvent(
            @JsonProperty("cacheName") String cacheName,
            @JsonProperty("key") Object key,
            @JsonProperty("value") Object value,
            @JsonProperty("eventType") EventType eventType) {
        this.cacheName = cacheName;
        this.key = key;
        this.value = value;
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

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    /**
     * 事件类型枚举
     */
    public enum EventType {
        UPDATE, // 更新事件
        DELETE, // 删除事件
        CLEAR // 清除事件
    }
}