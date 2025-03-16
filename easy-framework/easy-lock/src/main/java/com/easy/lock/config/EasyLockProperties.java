package com.easy.lock.config;

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
     * 锁前缀，用于区分不同应用
     */
    private String prefix = "easy:lock";
    
    /**
     * 默认过期时间（毫秒）
     */
    private long expireTime = 30000;
    
    /**
     * 默认重试次数
     */
    private int retryCount = 3;
    
    /**
     * 默认重试间隔（毫秒）
     */
    private long retryInterval = 100;
    
    /**
     * 是否启用锁监控
     */
    private boolean monitorEnabled = true;
} 