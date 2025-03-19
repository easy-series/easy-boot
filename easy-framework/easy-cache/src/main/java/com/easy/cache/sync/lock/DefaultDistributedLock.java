package com.easy.cache.sync.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认分布式锁实现
 * 注意：这是一个简单的实现，仅适用于单节点场景
 * 在生产环境中，建议使用Redis或Zookeeper实现分布式锁
 */
public class DefaultDistributedLock implements DistributedLock {
    private final ConcurrentMap<String, LockEntry> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key) {
        return tryLock(key, 30); // 默认30秒超时
    }

    @Override
    public boolean tryLock(String key, long timeoutSeconds) {
        LockEntry entry = locks.computeIfAbsent(key,
                k -> new LockEntry(new ReentrantLock(), System.currentTimeMillis() + timeoutSeconds * 1000));

        if (System.currentTimeMillis() > entry.expirationTime) {
            locks.remove(key);
            return false;
        }

        return entry.lock.tryLock();
    }

    @Override
    public void unlock(String key) {
        LockEntry entry = locks.get(key);
        if (entry != null) {
            if (System.currentTimeMillis() > entry.expirationTime) {
                locks.remove(key);
            } else {
                entry.lock.unlock();
            }
        }
    }

    private static class LockEntry {
        private final ReentrantLock lock;
        private final long expirationTime;

        public LockEntry(ReentrantLock lock, long expirationTime) {
            this.lock = lock;
            this.expirationTime = expirationTime;
        }
    }
}