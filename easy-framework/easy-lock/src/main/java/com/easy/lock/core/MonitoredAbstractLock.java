package com.easy.lock.core;

import com.easy.lock.monitor.LockMonitor;

import lombok.Setter;

/**
 * 带监控功能的抽象锁实现
 */
public abstract class MonitoredAbstractLock extends AbstractLock {

    /**
     * 锁监控器
     */
    @Setter
    private LockMonitor lockMonitor;

    @Override
    public LockInfo tryLock(String key, long expireTime, int retryCount, long retryInterval) {
        long startTime = System.currentTimeMillis();
        LockInfo lockInfo = super.tryLock(key, expireTime, retryCount, retryInterval);

        // 如果有监控器，记录锁操作结果
        if (lockMonitor != null) {
            if (lockInfo != null) {
                // 获取锁成功
                lockMonitor.recordSuccess(key);
            } else {
                // 获取锁失败
                lockMonitor.recordFail(key);
            }
        }

        return lockInfo;
    }

    @Override
    public boolean releaseLock(LockInfo lockInfo) {
        if (lockInfo == null) {
            return false;
        }

        long heldTime = 0;
        if (lockInfo.getAcquireTime() > 0) {
            heldTime = System.currentTimeMillis() - lockInfo.getAcquireTime();
        }

        boolean result = super.releaseLock(lockInfo);

        // 如果有监控器，记录锁释放结果
        if (lockMonitor != null) {
            if (result) {
                lockMonitor.recordReleaseSuccess();
                if (heldTime > 0) {
                    lockMonitor.recordLockTime(lockInfo.getKey(), heldTime);
                }
            } else {
                lockMonitor.recordReleaseFail();
            }
        }

        return result;
    }
}