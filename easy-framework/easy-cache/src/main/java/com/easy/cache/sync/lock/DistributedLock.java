package com.easy.cache.sync.lock;

/**
 * 分布式锁接口
 */
public interface DistributedLock {

    /**
     * 尝试获取锁
     * 
     * @param key 锁的键
     * @return 是否获取成功
     */
    boolean tryLock(String key);

    /**
     * 尝试获取锁，带超时时间
     * 
     * @param key            锁的键
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否获取成功
     */
    boolean tryLock(String key, long timeoutSeconds);

    /**
     * 释放锁
     * 
     * @param key 锁的键
     */
    void unlock(String key);
}