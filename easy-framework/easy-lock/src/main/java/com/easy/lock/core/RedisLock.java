package com.easy.lock.core;

import com.easy.lock.core.executor.LockExecutor;
import com.easy.lock.core.executor.RedisLockExecutor;

import lombok.RequiredArgsConstructor;

/**
 * Redis锁实现
 */
@RequiredArgsConstructor
public class RedisLock extends MonitoredAbstractLock {

    private final RedisLockExecutor redisLockExecutor;

    @Override
    protected LockExecutor getLockExecutor() {
        return redisLockExecutor;
    }

    @Override
    protected LockInfo.LockType getLockType() {
        return LockInfo.LockType.REDIS;
    }
}