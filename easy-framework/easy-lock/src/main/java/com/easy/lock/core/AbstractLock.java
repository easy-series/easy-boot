package com.easy.lock.core;

import com.easy.lock.core.executor.LockExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 抽象锁实现
 */
@Slf4j
public abstract class AbstractLock implements Lock {

    /**
     * 获取锁执行器
     *
     * @return 锁执行器
     */
    protected abstract LockExecutor getLockExecutor();

    /**
     * 获取锁类型
     *
     * @return 锁类型
     */
    protected abstract LockInfo.LockType getLockType();

    @Override
    public LockInfo tryLock(String key, long expireTime, int retryCount, long retryInterval) {
        String value = UUID.randomUUID().toString();
        LockExecutor lockExecutor = getLockExecutor();

        // 尝试获取锁
        boolean acquired = false;
        Exception exception = null;

        try {
            acquired = lockExecutor.acquire(key, value, expireTime);

            // 如果第一次获取锁失败，则重试指定次数
            for (int i = 0; !acquired && i < retryCount; i++) {
                log.debug("获取锁失败，进行第{}次重试，key={}", (i + 1), key);
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                acquired = lockExecutor.acquire(key, value, expireTime);
            }
        } catch (Exception e) {
            log.error("获取锁时发生异常，key={}", key, e);
            exception = e;
        }

        // 如果获取锁成功，构建并返回锁信息
        if (acquired) {
            long acquireTime = System.currentTimeMillis();
            return new LockInfo()
                    .setKey(key)
                    .setValue(value)
                    .setExpireTime(expireTime)
                    .setAcquireTime(acquireTime)
                    .setState(LockInfo.LockState.LOCKED)
                    .setType(getLockType());
        }

        if (exception != null) {
            log.warn("获取锁失败并发生异常，key={}", key, exception);
        } else {
            log.warn("获取锁失败，key={}", key);
        }

        return null;
    }

    @Override
    public boolean releaseLock(LockInfo lockInfo) {
        if (lockInfo == null || LockInfo.LockState.UNLOCKED.equals(lockInfo.getState())) {
            return false;
        }

        try {
            boolean released = getLockExecutor().release(lockInfo.getKey(), lockInfo.getValue());
            if (released) {
                lockInfo.setReleaseTime(System.currentTimeMillis());
                lockInfo.setState(LockInfo.LockState.UNLOCKED);
                return true;
            }
        } catch (Exception e) {
            log.error("释放锁时发生异常，key={}", lockInfo.getKey(), e);
        }

        return false;
    }

    @Override
    public boolean isLocked(String key) {
        try {
            return getLockExecutor().isLocked(key);
        } catch (Exception e) {
            log.error("检查锁状态时发生异常，key={}", key, e);
            return false;
        }
    }
}