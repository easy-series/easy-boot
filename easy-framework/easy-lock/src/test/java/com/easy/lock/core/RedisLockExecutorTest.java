package com.easy.lock.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.easy.lock.BaseLockTest;
import com.easy.lock.core.executor.RedisLockExecutor;

/**
 * Redis锁执行器测试类
 */
public class RedisLockExecutorTest extends BaseLockTest {

    @Autowired
    private RedisLockExecutor redisLockExecutor;

    @Test
    public void testAcquireAndRelease() {
        // 准备测试数据
        String key = "test:lock:" + UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();
        long expire = 10000;

        // 测试获取锁
        boolean acquired = redisLockExecutor.acquire(key, value, expire);
        assertTrue("应该成功获取锁", acquired);

        // 测试锁已被占用
        boolean acquiredAgain = redisLockExecutor.acquire(key, "other-value", expire);
        assertFalse("已有锁，应该获取失败", acquiredAgain);

        // 测试释放锁
        boolean released = redisLockExecutor.release(key, value);
        assertTrue("应该成功释放锁", released);

        // 测试锁释放后可再次获取
        boolean acquiredAfterRelease = redisLockExecutor.acquire(key, value, expire);
        assertTrue("锁释放后应该能再次获取", acquiredAfterRelease);

        // 清理
        redisLockExecutor.release(key, value);
    }

    @Test
    public void testLockExpire() throws InterruptedException {
        // 准备测试数据
        String key = "test:lock:expire:" + UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();
        // 设置一个非常短的过期时间
        long expire = 500; // 500毫秒

        // 获取锁
        boolean acquired = redisLockExecutor.acquire(key, value, expire);
        assertTrue("应该成功获取锁", acquired);

        // 等待锁过期
        Thread.sleep(1000);

        // 过期后应该可以再次获取
        boolean acquiredAfterExpire = redisLockExecutor.acquire(key, "other-value", expire);
        assertTrue("锁过期后应该能再次获取", acquiredAfterExpire);

        // 清理
        redisLockExecutor.release(key, "other-value");
    }

    @Test
    public void testIsLocked() {
        // 准备测试数据
        String key = "test:lock:check:" + UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();
        long expire = 10000;

        // 初始状态未加锁
        boolean lockedBefore = redisLockExecutor.isLocked(key);
        assertFalse("初始状态应该未加锁", lockedBefore);

        // 获取锁
        boolean acquired = redisLockExecutor.acquire(key, value, expire);
        assertTrue("应该成功获取锁", acquired);

        // 加锁后应返回true
        boolean lockedAfter = redisLockExecutor.isLocked(key);
        assertTrue("加锁后应该返回已锁定", lockedAfter);

        // 清理
        redisLockExecutor.release(key, value);
    }

    @Test
    public void testConcurrentLock() throws InterruptedException {
        final String key = "test:concurrent:lock:" + UUID.randomUUID().toString();
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 启动多个线程并发获取锁
        for (int i = 0; i < threadCount; i++) {
            final String value = "thread-" + i;
            executor.submit(() -> {
                try {
                    boolean success = redisLockExecutor.acquire(key, value, 5000);
                    if (success) {
                        // 获取锁成功，增加计数
                        try {
                            successCount.incrementAndGet();
                            // 模拟业务处理
                            Thread.sleep(100);
                        } finally {
                            redisLockExecutor.release(key, value);
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