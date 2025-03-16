package com.easy.cache.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 缓存框架配置属性
 */
@Data
@ConfigurationProperties(prefix = "easy.cache")
public class EasyCacheProperties {

    /**
     * 是否启用缓存
     */
    private boolean enabled = true;

    /**
     * 缓存同步通道名称
     */
    private String syncChannel = "easy:cache:sync";

    /**
     * 默认过期时间（秒）
     */
    private long defaultExpire = 3600;

    /**
     * 默认本地缓存大小限制
     */
    private int defaultLocalLimit = 10000;

    /**
     * 是否默认开启本地缓存同步
     */
    private boolean defaultSyncLocal = true;

    /**
     * 是否默认缓存空值
     */
    private boolean defaultCacheNullValues = true;

    /**
     * 是否默认开启穿透保护
     */
    private boolean defaultPenetrationProtect = false;

    /**
     * 是否默认开启直写模式
     */
    private boolean defaultWriteThrough = false;

    /**
     * 是否默认开启异步写入
     */
    private boolean defaultAsyncWrite = false;
}