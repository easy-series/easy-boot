package com.easy.easylock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式锁配置属性
 */
@Data
@ConfigurationProperties(prefix = "easy.lock")
public class EasyLockProperties {

    /**
     * 是否启用分布式锁
     */
    private boolean enabled = true;

    /**
     * 锁键前缀
     */
    private String keyPrefix = "easy:lock:";

    /**
     * 默认获取锁等待时间（毫秒）
     */
    private long defaultWaitTime = 3000;

    /**
     * 默认锁持有时间（毫秒）
     */
    private long defaultLeaseTime = 30000;
}