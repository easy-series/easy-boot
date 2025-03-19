package com.easy.cache.core;

import lombok.Builder;
import lombok.Data;

/**
 * 缓存配置类
 */
@Builder
@Data
public class CacheConfig {
    private long localExpireSeconds = 60;
    private long remoteExpireSeconds = 300;
    private int localMaxSize = 1000;
    private int remoteMaxSize = 10000;
    private int expireTime = 10000;
    private int maxSize = 10000;
    private boolean local = true;
    private boolean enableStats = true;

    public long getLocalExpireSeconds() {
        return localExpireSeconds;
    }

    public void setLocalExpireSeconds(long localExpireSeconds) {
        this.localExpireSeconds = localExpireSeconds;
    }

    public long getRemoteExpireSeconds() {
        return remoteExpireSeconds;
    }

    public void setRemoteExpireSeconds(long remoteExpireSeconds) {
        this.remoteExpireSeconds = remoteExpireSeconds;
    }

    public int getLocalMaxSize() {
        return localMaxSize;
    }

    public void setLocalMaxSize(int localMaxSize) {
        this.localMaxSize = localMaxSize;
    }

    public int getRemoteMaxSize() {
        return remoteMaxSize;
    }

    public void setRemoteMaxSize(int remoteMaxSize) {
        this.remoteMaxSize = remoteMaxSize;
    }
}