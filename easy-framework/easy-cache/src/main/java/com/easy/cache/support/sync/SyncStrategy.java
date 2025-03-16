package com.easy.cache.support.sync;

/**
 * 缓存同步策略枚举
 */
public enum SyncStrategy {
    /**
     * 立即同步
     */
    IMMEDIATELY,
    
    /**
     * 周期性同步
     */
    PERIODIC,
    
    /**
     * 批量同步
     */
    BATCH
}