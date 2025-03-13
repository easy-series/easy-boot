package com.easy.cache.core;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 */
public class CacheConfig {

    private String name;
    private long expireTime = 0;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    private boolean allowNullValues = false;
    private boolean syncLocal = false;

    public CacheConfig() {
    }

    public CacheConfig(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public CacheConfig setName(String name) {
        this.name = name;
        return this;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public CacheConfig setExpireTime(long expireTime) {
        this.expireTime = expireTime;
        return this;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public CacheConfig setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    public boolean isAllowNullValues() {
        return allowNullValues;
    }

    public CacheConfig setAllowNullValues(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
        return this;
    }

    public boolean isSyncLocal() {
        return syncLocal;
    }

    public CacheConfig setSyncLocal(boolean syncLocal) {
        this.syncLocal = syncLocal;
        return this;
    }

    /**
     * 设置过期时间
     * 
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     * @return 当前配置实例
     */
    public CacheConfig expire(long expireTime, TimeUnit timeUnit) {
        this.expireTime = expireTime;
        this.timeUnit = timeUnit;
        return this;
    }
}