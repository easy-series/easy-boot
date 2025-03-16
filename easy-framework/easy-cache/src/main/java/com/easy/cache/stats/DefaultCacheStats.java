package com.easy.cache.stats;

import com.easy.cache.support.stats.CacheStats;

/**
 * 默认的缓存统计实现类，继承自CacheStats
 */
public class DefaultCacheStats extends CacheStats {

    /**
     * 构造方法
     */
    public DefaultCacheStats() {
        super("default");
    }

    /**
     * 构造方法
     *
     * @param cacheName 缓存名称
     */
    public DefaultCacheStats(String cacheName) {
        super(cacheName);
    }
}