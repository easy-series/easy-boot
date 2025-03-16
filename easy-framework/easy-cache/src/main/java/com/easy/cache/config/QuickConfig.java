package com.easy.cache.config;

import java.time.Duration;

import com.easy.cache.support.sync.SyncStrategy;

import lombok.Data;

/**
 * 快速配置类，提供简便的配置方式
 */
@Data
public class QuickConfig {

    /**
     * 缓存名称
     */
    private String name;

    /**
     * 缓存类型
     */
    private CacheType cacheType;

    /**
     * 过期时间
     */
    private Duration expire;

    /**
     * 本地缓存条目数量限制
     */
    private int localLimit;

    /**
     * 是否启用本地缓存同步
     */
    private boolean syncLocal;

    /**
     * 缓存同步策略
     */
    private SyncStrategy syncStrategy;

    /**
     * 是否缓存null值
     */
    private boolean cacheNullValues;

    /**
     * 是否启用缓存穿透保护
     */
    private boolean penetrationProtect;

    /**
     * 是否使用写透策略（先写远程，再写本地）
     */
    private boolean writeThrough;

    /**
     * 是否异步写入远程缓存
     */
    private boolean asyncWrite;

    private QuickConfig(String name) {
        this.name = name;
        this.cacheType = CacheType.LOCAL;
        this.expire = Duration.ofMinutes(30);
        this.localLimit = 10000;
        this.syncLocal = false;
        this.syncStrategy = SyncStrategy.IMMEDIATELY;
        this.cacheNullValues = false;
        this.penetrationProtect = false;
        this.writeThrough = true;
        this.asyncWrite = false;
    }

    /**
     * 创建快速配置构建器
     *
     * @param name 缓存名称
     * @return 构建器
     */
    public static Builder newBuilder(String name) {
        return new Builder(name);
    }

    /**
     * 快速配置构建器
     */
    public static class Builder {
        private final QuickConfig config;

        private Builder(String name) {
            this.config = new QuickConfig(name);
        }

        /**
         * 设置缓存类型
         *
         * @param cacheType 缓存类型
         * @return 当前构建器
         */
        public Builder cacheType(CacheType cacheType) {
            config.setCacheType(cacheType);
            return this;
        }

        /**
         * 设置过期时间
         *
         * @param expire 过期时间
         * @return 当前构建器
         */
        public Builder expire(Duration expire) {
            config.setExpire(expire);
            return this;
        }

        /**
         * 设置本地缓存条目数量限制
         *
         * @param localLimit 限制数量
         * @return 当前构建器
         */
        public Builder localLimit(int localLimit) {
            config.setLocalLimit(localLimit);
            return this;
        }

        /**
         * 设置是否启用本地缓存同步
         *
         * @param syncLocal 是否启用本地缓存同步
         * @return 当前构建器
         */
        public Builder syncLocal(boolean syncLocal) {
            config.setSyncLocal(syncLocal);
            return this;
        }

        /**
         * 设置缓存同步策略
         *
         * @param syncStrategy 同步策略
         * @return 当前构建器
         */
        public Builder syncStrategy(SyncStrategy syncStrategy) {
            config.setSyncStrategy(syncStrategy);
            return this;
        }

        /**
         * 设置是否缓存null值
         *
         * @param cacheNullValues 是否缓存null值
         * @return 当前构建器
         */
        public Builder cacheNullValues(boolean cacheNullValues) {
            config.setCacheNullValues(cacheNullValues);
            return this;
        }

        /**
         * 设置是否启用缓存穿透保护
         *
         * @param penetrationProtect 是否启用缓存穿透保护
         * @return 当前构建器
         */
        public Builder penetrationProtect(boolean penetrationProtect) {
            config.setPenetrationProtect(penetrationProtect);
            return this;
        }

        /**
         * 设置是否使用写透策略
         *
         * @param writeThrough 是否使用写透策略
         * @return 当前构建器
         */
        public Builder writeThrough(boolean writeThrough) {
            config.setWriteThrough(writeThrough);
            return this;
        }

        /**
         * 设置是否异步写入远程缓存
         *
         * @param asyncWrite 是否异步写入远程缓存
         * @return 当前构建器
         */
        public Builder asyncWrite(boolean asyncWrite) {
            config.setAsyncWrite(asyncWrite);
            return this;
        }

        /**
         * 构建配置实例
         *
         * @return 配置实例
         */
        public QuickConfig build() {
            return config;
        }
    }
}