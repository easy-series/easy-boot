package com.easy.easylock.core;

/**
 * 分布式锁执行器
 */
public interface LockExecutor {

    /**
     * 尝试获取锁
     *
     * @param lock 锁对象
     * @return 获取锁的结果，true表示获取锁成功，false表示获取锁失败
     */
    boolean tryLock(Lock lock);

    /**
     * 释放锁
     *
     * @param lock 锁对象
     * @return 释放锁的结果，true表示释放成功，false表示释放失败
     */
    boolean releaseLock(Lock lock);
} 