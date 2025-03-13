package com.easy.easylock.utils;

import com.easy.easylock.core.Lock;
import com.easy.easylock.core.LockExecutor;
import com.easy.easylock.exception.LockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类，提供编程式API使用锁
 */
@Slf4j
@Component
public class LockUtil {

    private static LockExecutor lockExecutor;

    @Autowired
    public void setLockExecutor(LockExecutor lockExecutor) {
        LockUtil.lockExecutor = lockExecutor;
    }

    /**
     * 执行带锁的操作
     *
     * @param name     锁名称
     * @param key      锁的key
     * @param supplier 要执行的操作
     * @param <T>      返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithLock(String name, String key, Supplier<T> supplier) {
        return executeWithLock(name, key, 3000, 30000, TimeUnit.MILLISECONDS, true, supplier);
    }

    /**
     * 执行带锁的操作
     *
     * @param name           锁名称
     * @param key            锁的key
     * @param waitTime       获取锁等待时间
     * @param leaseTime      锁持有时间
     * @param timeUnit       时间单位
     * @param throwException 获取锁失败时是否抛出异常
     * @param supplier       要执行的操作
     * @param <T>            返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithLock(String name, String key, long waitTime, long leaseTime,
            TimeUnit timeUnit, boolean throwException, Supplier<T> supplier) {
        // 创建锁对象
        Lock lock = new Lock()
                .setName(name)
                .setKey(key)
                .setValue(UUID.randomUUID().toString())
                .setWaitTime(waitTime)
                .setLeaseTime(leaseTime)
                .setTimeUnit(timeUnit)
                .setThrowException(throwException)
                .setFailMessage("获取分布式锁失败：" + name);

        boolean locked = false;
        try {
            // 尝试获取锁
            locked = lockExecutor.tryLock(lock);

            if (!locked) {
                // 获取锁失败
                if (throwException) {
                    throw new LockException(lock.getFailMessage());
                }
                log.warn("获取分布式锁失败: {}", lock.getFullName());
                return null;
            }

            // 获取锁成功，执行操作
            return supplier.get();
        } finally {
            // 释放锁
            if (locked) {
                boolean released = lockExecutor.releaseLock(lock);
                if (!released) {
                    log.warn("释放分布式锁失败: {}", lock.getFullName());
                }
            }
        }
    }

    /**
     * 执行带锁的操作（无返回值）
     *
     * @param name     锁名称
     * @param key      锁的key
     * @param runnable 要执行的操作
     */
    public static void executeWithLock(String name, String key, Runnable runnable) {
        executeWithLock(name, key, () -> {
            runnable.run();
            return null;
        });
    }
}