package com.easy.lock.core;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 锁信息
 */
@Data
@Accessors(chain = true)
public class LockInfo {

    /**
     * 锁的名称
     */
    private String name;

    /**
     * 锁的键
     */
    private String key;

    /**
     * 锁的值，用于解锁验证
     */
    private String value;

    /**
     * 锁的到期时间（毫秒）
     */
    private long expireTime;

    /**
     * 获取锁的时间
     */
    private long acquireTime;

    /**
     * 释放锁的时间
     */
    private long releaseTime;

    /**
     * 锁的状态
     */
    private LockState state = LockState.UNLOCKED;

    /**
     * 锁的类型
     */
    private LockType type;

    /**
     * 锁类型枚举
     */
    public enum LockType {
        /**
         * Redis实现
         */
        REDIS,

        /**
         * ZooKeeper实现
         */
        ZOOKEEPER,

        /**
         * ETCD实现
         */
        ETCD
    }

    /**
     * 锁状态枚举
     */
    public enum LockState {
        /**
         * 已解锁
         */
        UNLOCKED,

        /**
         * 已锁定
         */
        LOCKED
    }
}