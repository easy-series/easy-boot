package com.easy.lock.template;

import com.easy.lock.core.Lock;
import com.easy.lock.core.LockInfo;
import com.easy.lock.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 锁模板
 */
@Slf4j
@RequiredArgsConstructor
public class LockTemplate {

    private final Lock lock;
    
    /**
     * 获取分布式锁的默认超时时间（毫秒）
     */
    private static final long DEFAULT_EXPIRE_TIME = 30000L;
    
    /**
     * 获取分布式锁的默认重试次数
     */
    private static final int DEFAULT_RETRY_COUNT = 3;
    
    /**
     * 获取分布式锁的默认重试间隔（毫秒）
     */
    private static final long DEFAULT_RETRY_INTERVAL = 100L;

    /**
     * 使用默认参数执行加锁操作
     *
     * @param key 锁的键
     * @param supplier 获取锁后执行的操作
     * @param <T> 返回值类型
     * @return 操作执行结果
     */
    public <T> T lock(String key, Supplier<T> supplier) {
        return lock(key, DEFAULT_EXPIRE_TIME, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_INTERVAL, supplier);
    }

    /**
     * 使用默认重试参数执行加锁操作
     *
     * @param key 锁的键
     * @param expireTime 锁的过期时间
     * @param supplier 获取锁后执行的操作
     * @param <T> 返回值类型
     * @return 操作执行结果
     */
    public <T> T lock(String key, long expireTime, Supplier<T> supplier) {
        return lock(key, expireTime, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_INTERVAL, supplier);
    }

    /**
     * 使用默认重试间隔执行加锁操作
     *
     * @param key 锁的键
     * @param expireTime 锁的过期时间
     * @param retryCount 重试次数
     * @param supplier 获取锁后执行的操作
     * @param <T> 返回值类型
     * @return 操作执行结果
     */
    public <T> T lock(String key, long expireTime, int retryCount, Supplier<T> supplier) {
        return lock(key, expireTime, retryCount, DEFAULT_RETRY_INTERVAL, supplier);
    }

    /**
     * 执行加锁操作
     *
     * @param key 锁的键
     * @param expireTime 锁的过期时间
     * @param retryCount 重试次数
     * @param retryInterval 重试间隔
     * @param supplier 获取锁后执行的操作
     * @param <T> 返回值类型
     * @return 操作执行结果
     */
    public <T> T lock(String key, long expireTime, int retryCount, long retryInterval, Supplier<T> supplier) {
        LockInfo lockInfo = null;
        try {
            lockInfo = lock.tryLock(key, expireTime, retryCount, retryInterval);
            if (lockInfo == null) {
                throw new LockException("获取锁失败，key = " + key);
            }
            return supplier.get();
        } finally {
            unlock(lockInfo);
        }
    }

    /**
     * 仅获取锁
     *
     * @param key 锁的键
     * @return 锁信息，获取失败时返回null
     */
    public LockInfo tryLock(String key) {
        return tryLock(key, DEFAULT_EXPIRE_TIME, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_INTERVAL);
    }

    /**
     * 仅获取锁，自定义参数
     *
     * @param key 锁的键
     * @param expireTime 锁的过期时间
     * @param retryCount 重试次数
     * @param retryInterval 重试间隔
     * @return 锁信息，获取失败时返回null
     */
    public LockInfo tryLock(String key, long expireTime, int retryCount, long retryInterval) {
        return lock.tryLock(key, expireTime, retryCount, retryInterval);
    }

    /**
     * 释放锁
     *
     * @param lockInfo 锁信息
     * @return 是否释放成功
     */
    public boolean unlock(LockInfo lockInfo) {
        if (lockInfo == null) {
            return false;
        }
        return lock.releaseLock(lockInfo);
    }
}