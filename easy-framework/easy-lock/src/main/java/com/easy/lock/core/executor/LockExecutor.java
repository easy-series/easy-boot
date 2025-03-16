package com.easy.lock.core.executor;

/**
 * 锁执行器接口
 */
public interface LockExecutor {

    /**
     * 尝试获取锁
     *
     * @param key    锁键
     * @param value  锁值，确保解锁时是同一个线程持有者
     * @param expire 获取锁超时时间
     * @return 获取锁成功返回true，否则返回false
     */
    boolean acquire(String key, String value, long expire);

    /**
     * 释放锁
     *
     * @param key   锁键
     * @param value 锁值，确保解锁时是同一个线程持有者
     * @return 释放锁成功返回true，否则返回false
     */
    boolean release(String key, String value);

    /**
     * 检查锁是否被占用
     *
     * @param key 锁键
     * @return 锁被占用返回true，否则返回false
     */
    boolean isLocked(String key);

}