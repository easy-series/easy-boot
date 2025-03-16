package com.easy.cache.config;

import java.util.concurrent.TimeUnit;

/**
 * 缓存刷新策略
 */
public class RefreshPolicy {
    
    /**
     * 刷新间隔
     */
    private long refreshInterval;
    
    /**
     * 时间单位
     */
    private TimeUnit timeUnit;
    
    /**
     * 最后访问后停止刷新的时间
     */
    private long stopRefreshAfterLastAccess;
    
    /**
     * 创建新的刷新策略
     * 
     * @param refreshInterval 刷新间隔
     * @param timeUnit 时间单位
     * @return 刷新策略
     */
    public static RefreshPolicy newPolicy(long refreshInterval, TimeUnit timeUnit) {
        RefreshPolicy policy = new RefreshPolicy();
        policy.refreshInterval = refreshInterval;
        policy.timeUnit = timeUnit;
        return policy;
    }
    
    /**
     * 设置最后访问后停止刷新的时间
     * 
     * @param time 时间
     * @param unit 时间单位
     * @return 当前策略
     */
    public RefreshPolicy stopRefreshAfterLastAccess(long time, TimeUnit unit) {
        this.stopRefreshAfterLastAccess = unit.toMillis(time);
        return this;
    }
    
    /**
     * 获取刷新间隔（毫秒）
     * 
     * @return 刷新间隔
     */
    public long getRefreshIntervalMillis() {
        return timeUnit.toMillis(refreshInterval);
    }
    
    /**
     * 获取最后访问后停止刷新的时间（毫秒）
     * 
     * @return 停止刷新时间
     */
    public long getStopRefreshAfterLastAccessMillis() {
        return stopRefreshAfterLastAccess;
    }
    
    /**
     * 获取刷新间隔
     * 
     * @return 刷新间隔
     */
    public long getRefreshInterval() {
        return refreshInterval;
    }
    
    /**
     * 获取时间单位
     * 
     * @return 时间单位
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
} 