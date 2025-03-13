package com.easy.easylock.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁模型
 */
@Data
@Accessors(chain = true)
public class Lock {

    /**
     * 锁名称
     */
    private String name;

    /**
     * 锁的key
     */
    private String key;

    /**
     * 锁的值，一般为唯一标识
     */
    private String value;

    /**
     * 获取锁等待时间
     */
    private long waitTime;

    /**
     * 锁的持有时间(过期时间)
     */
    private long leaseTime;

    /**
     * 时间单位
     */
    private TimeUnit timeUnit;

    /**
     * 获取锁失败时，是否抛出异常
     */
    private boolean throwException;

    /**
     * 获取锁失败时的错误消息
     */
    private String failMessage;

    /**
     * 获取完整的锁名称(包含key)
     */
    public String getFullName() {
        if (key != null && !key.isEmpty()) {
            return name + ":" + key;
        }
        return name;
    }
}