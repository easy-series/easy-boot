package com.easy.cache.support.sync;

import java.io.Serializable;

import lombok.Getter;

/**
 * 缓存事件
 */
@Getter
public class CacheEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 缓存名称
     */
    private final String cacheName;

    /**
     * 缓存键
     */
    private final String cacheKey;

    /**
     * 事件类型
     */
    private final CacheEventType eventType;

    /**
     * 私有构造函数
     */
    private CacheEvent(String cacheName, String cacheKey, CacheEventType eventType) {
        this.cacheName = cacheName;
        this.cacheKey = cacheKey;
        this.eventType = eventType;
    }

    /**
     * 创建更新事件
     *
     * @param cacheName 缓存名称
     * @param cacheKey  缓存键
     * @return 更新事件
     */
    public static CacheEvent createUpdateEvent(String cacheName, String cacheKey) {
        return new CacheEvent(cacheName, cacheKey, CacheEventType.UPDATE);
    }

    /**
     * 创建删除事件
     *
     * @param cacheName 缓存名称
     * @param cacheKey  缓存键
     * @return 删除事件
     */
    public static CacheEvent createRemoveEvent(String cacheName, String cacheKey) {
        return new CacheEvent(cacheName, cacheKey, CacheEventType.REMOVE);
    }

    /**
     * 创建清空事件
     *
     * @param cacheName 缓存名称
     * @return 清空事件
     */
    public static CacheEvent createClearEvent(String cacheName) {
        return new CacheEvent(cacheName, null, CacheEventType.CLEAR);
    }

    @Override
    public String toString() {
        return "CacheEvent{" +
                "cacheName='" + cacheName + '\'' +
                ", cacheKey='" + cacheKey + '\'' +
                ", eventType=" + eventType +
                '}';
    }
}