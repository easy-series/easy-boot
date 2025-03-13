package com.easy.cache.core;

import java.util.concurrent.TimeUnit;

/**
 * 快速缓存配置类，用于简化缓存创建
 */
public class QuickConfig {

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

    private final String name;
    private final long expire;
    private final TimeUnit timeUnit;
    private final CacheType cacheType;
    private final boolean cacheNull;
    private final boolean refreshable;
    private final long refreshInterval;
    private final TimeUnit refreshTimeUnit;
    private final boolean writeThrough;
    private final boolean asyncWrite;
    private final boolean syncLocal;
    private final int localLimit;

    private QuickConfig(Builder builder) {
        this.name = builder.name;
        this.expire = builder.expire;
        this.timeUnit = builder.timeUnit;
        this.cacheType = builder.cacheType;
        this.cacheNull = builder.cacheNull;
        this.refreshable = builder.refreshable;
        this.refreshInterval = builder.refreshInterval;
        this.refreshTimeUnit = builder.refreshTimeUnit;
        this.writeThrough = builder.writeThrough;
        this.asyncWrite = builder.asyncWrite;
        this.syncLocal = builder.syncLocal;
        this.localLimit = builder.localLimit;
    }

    /**
     * 创建构建器
     * 
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取缓存名称
     * 
     * @return 缓存名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取过期时间
     * 
     * @return 过期时间
     */
    public long getExpire() {
        return expire;
    }

    /**
     * 获取过期时间单位
     * 
     * @return 过期时间单位
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * 获取缓存类型
     * 
     * @return 缓存类型
     */
    public CacheType getCacheType() {
        return cacheType;
    }

    /**
     * 是否缓存空值
     * 
     * @return 是否缓存空值
     */
    public boolean isCacheNull() {
        return cacheNull;
    }

    /**
     * 是否启用自动刷新
     * 
     * @return 是否启用自动刷新
     */
    public boolean isRefreshable() {
        return refreshable;
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
     * 获取刷新间隔时间单位
     * 
     * @return 刷新间隔时间单位
     */
    public TimeUnit getRefreshTimeUnit() {
        return refreshTimeUnit;
    }

    /**
     * 是否启用写透模式
     * 
     * @return 是否启用写透模式
     */
    public boolean isWriteThrough() {
        return writeThrough;
    }

    /**
     * 是否启用异步写入
     * 
     * @return 是否启用异步写入
     */
    public boolean isAsyncWrite() {
        return asyncWrite;
    }

    /**
     * 是否同步本地缓存
     * 
     * @return 是否同步本地缓存
     */
    public boolean isSyncLocal() {
        return syncLocal;
    }

    /**
     * 获取本地缓存大小限制
     * 
     * @return 本地缓存大小限制
     */
    public int getLocalLimit() {
        return localLimit;
    }

    /**
     * 构建器类
     */
    public static class Builder {
        private String name;
        private long expire = 0;
        private TimeUnit timeUnit = TimeUnit.SECONDS;
        private CacheType cacheType = CacheType.LOCAL;
        private boolean cacheNull = false;
        private boolean refreshable = false;
        private long refreshInterval = 1;
        private TimeUnit refreshTimeUnit = TimeUnit.MINUTES;
        private boolean writeThrough = false;
        private boolean asyncWrite = false;
        private boolean syncLocal = false;
        private int localLimit = 10000;

        /**
         * 设置缓存名称
         * 
         * @param name 缓存名称
         * @return 构建器实例
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置过期时间
         * 
         * @param expire    过期时间
         * @param timeUnit  时间单位
         * @return 构建器实例
         */
        public Builder expire(long expire, TimeUnit timeUnit) {
            this.expire = expire;
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * 设置缓存类型
         * 
         * @param cacheType 缓存类型
         * @return 构建器实例
         */
        public Builder cacheType(CacheType cacheType) {
            this.cacheType = cacheType;
            return this;
        }

        /**
         * 设置是否缓存空值
         * 
         * @param cacheNull 是否缓存空值
         * @return 构建器实例
         */
        public Builder cacheNull(boolean cacheNull) {
            this.cacheNull = cacheNull;
            return this;
        }

        /**
         * 设置是否启用自动刷新
         * 
         * @param refreshable 是否启用自动刷新
         * @return 构建器实例
         */
        public Builder refreshable(boolean refreshable) {
            this.refreshable = refreshable;
            return this;
        }

        /**
         * 设置刷新间隔
         * 
         * @param refreshInterval 刷新间隔
         * @param refreshTimeUnit 刷新间隔时间单位
         * @return 构建器实例
         */
        public Builder refreshInterval(long refreshInterval, TimeUnit refreshTimeUnit) {
            this.refreshInterval = refreshInterval;
            this.refreshTimeUnit = refreshTimeUnit;
            return this;
        }

        /**
         * 设置是否启用写透模式
         * 
         * @param writeThrough 是否启用写透模式
         * @return 构建器实例
         */
        public Builder writeThrough(boolean writeThrough) {
            this.writeThrough = writeThrough;
            return this;
        }

        /**
         * 设置是否启用异步写入
         * 
         * @param asyncWrite 是否启用异步写入
         * @return 构建器实例
         */
        public Builder asyncWrite(boolean asyncWrite) {
            this.asyncWrite = asyncWrite;
            return this;
        }

        /**
         * 设置是否同步本地缓存（仅对多级缓存有效）
         * 
         * @param syncLocal 是否同步本地缓存
         * @return 构建器实例
         */
        public Builder syncLocal(boolean syncLocal) {
            this.syncLocal = syncLocal;
            return this;
        }

        /**
         * 设置本地缓存大小限制
         * 
         * @param localLimit 本地缓存大小限制
         * @return 构建器实例
         */
        public Builder localLimit(int localLimit) {
            this.localLimit = localLimit;
            return this;
        }

        /**
         * 构建 QuickConfig 实例
         * 
         * @return QuickConfig 实例
         */
        public QuickConfig build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("缓存名称不能为空");
            }
            return new QuickConfig(this);
        }
    }
}