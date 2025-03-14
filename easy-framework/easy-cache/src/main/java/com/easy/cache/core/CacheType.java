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
     * Redis缓存
     */
    REDIS,
    
    /**
     * 两级缓存(本地+Redis)
     */
    TWO_LEVEL
} 