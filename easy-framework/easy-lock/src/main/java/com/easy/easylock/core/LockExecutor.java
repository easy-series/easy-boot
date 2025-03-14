package com.easy.easylock.core;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁执行器接口
 * 定义了锁的获取与释放操作
 */
public interface LockExecutor {

    /**
     * 尝试获取锁
     *
     * @param lockKey   锁的键
     * @param lockValue 锁的值，通常用于标识锁的持有者
     * @param waitTime  获取锁的等待时间
     * @param leaseTime 锁的持有时间(过期时间)
     * @param timeUnit  时间单位
     * @return 是否成功获取锁
     */
    boolean tryLock(String lockKey, String lockValue, long waitTime, long leaseTime, TimeUnit timeUnit);

    /**
     * 释放锁
     *
     * @param lockKey   锁的键
     * @param lockValue 锁的值，必须与加锁时的值一致，确保锁只能被持有者释放
     * @return 是否成功释放锁
     */
    boolean releaseLock(String lockKey, String lockValue);
}