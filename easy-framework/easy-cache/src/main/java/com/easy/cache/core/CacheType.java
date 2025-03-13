package com.easy.cache.core;

/**
 * 缓存类型枚举
 */
public enum CacheType {
    /**
     * 本地缓存
     */
    LOCAL,

    /**
     * 远程缓存（Redis）
     */
    REMOTE,

    /**
     * 两级缓存（本地+远程）
     */
    BOTH
} 