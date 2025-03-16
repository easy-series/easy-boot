package com.easy.cache.support.sync;

/**
 * 缓存事件类型枚举
 */
public enum CacheEventType {
    /**
     * 新增/更新缓存
     */
    PUT,

    /**
     * 移除缓存
     */
    REMOVE,

    /**
     * 清空缓存
     */
    CLEAR,

    /**
     * 更新缓存（兼容旧版）
     */
    UPDATE
}