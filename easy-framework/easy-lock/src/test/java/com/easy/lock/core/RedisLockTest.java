package com.easy.lock.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.easy.lock.BaseLockTest;

/**
 * Redis锁测试类
 */
public class RedisLockTest extends BaseLockTest {

    @Autowired
    private RedisLock redisLock;

    @Test
    public void testTryLockAndRelease() {
        // 准备测试数据
        String lockKey = "test:redis:lock:" + UUID.randomUUID().toString();

        // 尝试获取锁
        LockInfo lockInfo = redisLock.tryLock(lockKey, 30000, 0, 0);
        assertNotNull("应该成功获取锁", lockInfo);
        assertEquals("锁类型应该是REDIS", LockInfo.LockType.REDIS, lockInfo.getType());
        assertEquals("锁键应该正确", lockKey, lockInfo.getKey());
        assertNotNull("锁值不应为空", lockInfo.getValue());

        // 检查是否已锁定
        boolean isLocked = redisLock.isLocked(lockKey);
        assertTrue("锁应该处于锁定状态", isLocked);

        // 释放锁
        boolean unlocked = redisLock.releaseLock(lockInfo);
        assertTrue("应该成功释放锁", unlocked);

        // 确认锁已释放
        isLocked = redisLock.isLocked(lockKey);
        assertFalse("锁应该已释放", isLocked);
    }

    @Test
    public void testTryLockWithRetry() throws InterruptedException {
        // 准备测试数据
        String lockKey = "test:redis:lock:retry:" + UUID.randomUUID().toString();

        // 先获取一个锁，使第一次尝试失败
        LockInfo firstLock = redisLock.tryLock(lockKey, 5000, 0, 0);
        assertNotNull("应该成功获取第一个锁", firstLock);

        // 尝试在单独线程中获取锁，应该会重试
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                // 尝试获取锁，设置重试3次，间隔200ms
                LockInfo lockInfo = redisLock.tryLock(lockKey, 5000, 3, 200);
                if (lockInfo != null) {
                    try {
                        successCount.incrementAndGet();
                    } finally {
                        redisLock.releaseLock(lockInfo);
                    }
                }
            } finally {
                latch.countDown();
            }
        });

        // 等待一段时间后释放第一个锁，让重试能成功
        Thread.sleep(300);
        redisLock.releaseLock(firstLock);

        // 等待重试线程完成
        latch.await();
        executor.shutdown();

        // 验证最终获取成功
        assertEquals("应该通过重试成功获取锁", 1, successCount.get());
    }

    @Test
    public void testTryLockFail() {
        // 准备测试数据
        String lockKey = "test:redis:lock:fail:" + UUID.randomUUID().toString();

        // 先获取一个锁
        LockInfo firstLock = redisLock.tryLock(lockKey, 30000, 0, 0);
        assertNotNull("应该成功获取第一个锁", firstLock);

        try {
            // 尝试再次获取同一个锁，不重试
            LockInfo secondLock = redisLock.tryLock(lockKey, 30000, 0, 0);
            assertNull("应该获取锁失败", secondLock);
        } finally {
            // 释放第一个锁
            redisLock.releaseLock(firstLock);
        }
    }

    @Test
    public void testConcurrentLock() throws InterruptedException {
        final String lockKey = "test:redis:concurrent:lock:" + UUID.randomUUID().toString();
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 启动多个线程并发获取锁
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    LockInfo lockInfo = redisLock.tryLock(lockKey, 30000, 0, 0);
                    if (lockInfo != null) {
                        try {
                            // 获取锁成功，增加计数
                            successCount.incrementAndGet();
                            // 模拟业务处理
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            redisLock.releaseLock(lockInfo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程执行完毕
        latch.await();
        executor.shutdown();

        // 只有一个线程应该成功获取锁
        assertEquals("只应有一个线程成功获取锁", 1, successCount.get());
    }
}