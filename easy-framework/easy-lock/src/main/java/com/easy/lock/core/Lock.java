package com.easy.lock.core;

/**
 * 分布式锁接口
 */
public interface Lock {

    /**
     * 尝试加锁，若失败立即返回
     *
     * @param key         锁定的资源标识
     * @param expireTime  锁的过期时间(毫秒)
     * @param retryCount  重试次数
     * @param retryInterval 重试间隔(毫秒)
     * @return 锁信息，加锁失败时返回null
     */
    LockInfo tryLock(String key, long expireTime, int retryCount, long retryInterval);

    /**
     * 释放锁
     *
     * @param lockInfo 锁信息
     * @return 是否释放成功
     */
    boolean releaseLock(LockInfo lockInfo);
    
    /**
     * 锁是否被占用
     *
     * @param key 锁定的资源标识
     * @return 是否被占用
     */
    default boolean isLocked(String key) {
        return false;
    }
} 