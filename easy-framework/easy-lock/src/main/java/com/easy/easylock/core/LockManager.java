package com.easy.easylock.core;

import java.util.UUID;

import com.easy.easylock.core.factory.LockFactory;
import com.easy.easylock.exception.LockException;

import lombok.extern.slf4j.Slf4j;

/**
 * 锁管理器，提供分布式锁的获取和释放操作
 */
@Slf4j
public class LockManager {

    private final LockFactory lockFactory;

    /**
     * 构造函数
     *
     * @param lockFactory 锁工厂
     */
    public LockManager(LockFactory lockFactory) {
        this.lockFactory = lockFactory;
    }

    /**
     * 获取锁
     *
     * @param lock 锁对象
     * @return 获取锁的结果，包含锁的相关信息和获取状态
     */
    public LockResult acquire(Lock lock) {
        // 生成唯一锁值（用于标识锁的持有者）
        String lockValue = generateLockValue();

        // 完善锁对象的值
        lock.setValue(lockValue);

        // 获取锁执行器
        LockExecutor executor = lockFactory.getExecutor();

        // 尝试获取锁
        boolean acquired = executor.tryLock(
                lock.getFullName(),
                lockValue,
                lock.getWaitTime(),
                lock.getLeaseTime(),
                lock.getTimeUnit());

        // 创建锁结果对象
        LockResult result = new LockResult();
        result.setLock(lock);
        result.setAcquired(acquired);

        // 如果获取锁失败且需要抛出异常
        if (!acquired && lock.isThrowException()) {
            String errorMsg = lock.getFailMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "获取分布式锁失败: " + lock.getFullName();
            }
            throw new LockException(errorMsg);
        }

        return result;
    }

    /**
     * 释放锁
     *
     * @param result 锁结果对象
     * @return 是否成功释放锁
     */
    public boolean release(LockResult result) {
        // 如果没有获取到锁，直接返回true
        if (!result.isAcquired()) {
            return true;
        }

        Lock lock = result.getLock();

        // 获取锁执行器
        LockExecutor executor = lockFactory.getExecutor();

        // 释放锁
        boolean released = executor.releaseLock(lock.getFullName(), lock.getValue());

        if (!released) {
            log.warn("释放锁失败 - name: {}, value: {}", lock.getFullName(), lock.getValue());
        }

        return released;
    }

    /**
     * 生成唯一的锁值
     *
     * @return 锁值
     */
    private String generateLockValue() {
        return UUID.randomUUID().toString();
    }
}