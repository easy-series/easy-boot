package com.easy.easylock.template;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.easy.easylock.core.Lock;
import com.easy.easylock.core.LockManager;
import com.easy.easylock.core.LockResult;
import com.easy.easylock.exception.LockException;

import lombok.extern.slf4j.Slf4j;

/**
 * 锁模板类，提供手动加锁的方式
 */
@Slf4j
public class LockTemplate {

    private final LockManager lockManager;

    /**
     * 构造函数
     *
     * @param lockManager 锁管理器
     */
    public LockTemplate(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    /**
     * 获取锁
     *
     * @param name 锁名称
     * @return 锁结果
     */
    public LockResult lock(String name) {
        return lock(name, null);
    }

    /**
     * 获取锁
     *
     * @param name 锁名称
     * @param key  锁的键
     * @return 锁结果
     */
    public LockResult lock(String name, String key) {
        return lock(name, key, 3000L, 30000L, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取锁
     *
     * @param name      锁名称
     * @param key       锁的键
     * @param waitTime  获取锁的等待时间
     * @param leaseTime 锁的持有时间(过期时间)
     * @param timeUnit  时间单位
     * @return 锁结果
     */
    public LockResult lock(String name, String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        return lock(name, key, waitTime, leaseTime, timeUnit, true);
    }

    /**
     * 获取锁
     *
     * @param name           锁名称
     * @param key            锁的键
     * @param waitTime       获取锁的等待时间
     * @param leaseTime      锁的持有时间(过期时间)
     * @param timeUnit       时间单位
     * @param throwException 获取锁失败时是否抛出异常
     * @return 锁结果
     */
    public LockResult lock(String name, String key, long waitTime, long leaseTime, TimeUnit timeUnit,
            boolean throwException) {
        return lock(name, key, waitTime, leaseTime, timeUnit, throwException,
                "获取分布式锁失败: " + name + (key != null ? ":" + key : ""));
    }

    /**
     * 获取锁
     *
     * @param name           锁名称
     * @param key            锁的键
     * @param waitTime       获取锁的等待时间
     * @param leaseTime      锁的持有时间(过期时间)
     * @param timeUnit       时间单位
     * @param throwException 获取锁失败时是否抛出异常
     * @param failMessage    获取锁失败时的错误消息
     * @return 锁结果
     */
    public LockResult lock(String name, String key, long waitTime, long leaseTime, TimeUnit timeUnit,
            boolean throwException, String failMessage) {
        Lock lock = new Lock()
                .setName(name)
                .setKey(key)
                .setWaitTime(waitTime)
                .setLeaseTime(leaseTime)
                .setTimeUnit(timeUnit)
                .setThrowException(throwException)
                .setFailMessage(failMessage);
        return lockManager.acquire(lock);
    }

    /**
     * 尝试获取锁，不等待
     *
     * @param name 锁名称
     * @return 是否成功获取锁
     */
    public boolean tryLock(String name) {
        return tryLock(name, null);
    }

    /**
     * 尝试获取锁，不等待
     *
     * @param name 锁名称
     * @param key  锁的键
     * @return 是否成功获取锁
     */
    public boolean tryLock(String name, String key) {
        return tryLock(name, key, 0, 30000L, TimeUnit.MILLISECONDS);
    }

    /**
     * 尝试获取锁
     *
     * @param name      锁名称
     * @param key       锁的键
     * @param waitTime  获取锁的等待时间
     * @param leaseTime 锁的持有时间(过期时间)
     * @param timeUnit  时间单位
     * @return 是否成功获取锁
     */
    public boolean tryLock(String name, String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        LockResult result = lock(name, key, waitTime, leaseTime, timeUnit, false);
        return result.isAcquired();
    }

    /**
     * 释放锁
     *
     * @param lockResult 锁结果
     * @return 是否成功释放锁
     */
    public boolean unlock(LockResult lockResult) {
        return lockManager.release(lockResult);
    }

    /**
     * 在锁内执行操作
     *
     * @param name   锁名称
     * @param action 要执行的操作
     * @param <T>    返回类型
     * @return 操作的返回结果
     */
    public <T> T doInLock(String name, Supplier<T> action) {
        return doInLock(name, null, action);
    }

    /**
     * 在锁内执行操作
     *
     * @param name   锁名称
     * @param key    锁的键
     * @param action 要执行的操作
     * @param <T>    返回类型
     * @return 操作的返回结果
     */
    public <T> T doInLock(String name, String key, Supplier<T> action) {
        return doInLock(name, key, 3000L, 30000L, TimeUnit.MILLISECONDS, action);
    }

    /**
     * 在锁内执行操作
     *
     * @param name      锁名称
     * @param key       锁的键
     * @param waitTime  获取锁的等待时间
     * @param leaseTime 锁的持有时间(过期时间)
     * @param timeUnit  时间单位
     * @param action    要执行的操作
     * @param <T>       返回类型
     * @return 操作的返回结果
     */
    public <T> T doInLock(String name, String key, long waitTime, long leaseTime, TimeUnit timeUnit,
            Supplier<T> action) {
        LockResult lockResult = lock(name, key, waitTime, leaseTime, timeUnit);
        try {
            // 如果获取锁失败且不抛出异常(这里不会发生，因为lock方法中已经检查了是否抛出异常)
            if (!lockResult.isAcquired()) {
                throw new LockException("获取分布式锁失败: " + name + (key != null ? ":" + key : ""));
            }
            // 执行业务逻辑
            return action.get();
        } finally {
            // 释放锁
            unlock(lockResult);
        }
    }

    /**
     * 在锁内执行操作，无返回结果
     *
     * @param name   锁名称
     * @param action 要执行的操作
     */
    public void doInLock(String name, Runnable action) {
        doInLock(name, null, action);
    }

    /**
     * 在锁内执行操作，无返回结果
     *
     * @param name   锁名称
     * @param key    锁的键
     * @param action 要执行的操作
     */
    public void doInLock(String name, String key, Runnable action) {
        doInLock(name, key, 3000L, 30000L, TimeUnit.MILLISECONDS, action);
    }

    /**
     * 在锁内执行操作，无返回结果
     *
     * @param name      锁名称
     * @param key       锁的键
     * @param waitTime  获取锁的等待时间
     * @param leaseTime 锁的持有时间(过期时间)
     * @param timeUnit  时间单位
     * @param action    要执行的操作
     */
    public void doInLock(String name, String key, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable action) {
        LockResult lockResult = lock(name, key, waitTime, leaseTime, timeUnit);
        try {
            // 如果获取锁失败且不抛出异常(这里不会发生，因为lock方法中已经检查了是否抛出异常)
            if (!lockResult.isAcquired()) {
                throw new LockException("获取分布式锁失败: " + name + (key != null ? ":" + key : ""));
            }
            // 执行业务逻辑
            action.run();
        } finally {
            // 释放锁
            unlock(lockResult);
        }
    }
}