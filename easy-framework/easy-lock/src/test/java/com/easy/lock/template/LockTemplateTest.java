package com.easy.lock.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.easy.lock.BaseLockTest;
import com.easy.lock.core.LockInfo;
import com.easy.lock.exception.LockException;

/**
 * 锁模板测试类
 */
public class LockTemplateTest extends BaseLockTest {

    @Autowired
    private LockTemplate lockTemplate;

    private final Map<String, Integer> testData = new ConcurrentHashMap<>();

    @Test
    public void testLockAndExecute() {
        // 准备测试数据
        String resourceId = "test-resource-" + UUID.randomUUID().toString();
        String lockKey = "test:template:" + resourceId;

        // 执行带锁的业务逻辑
        boolean result = lockTemplate.lock(lockKey, () -> {
            // 模拟业务处理
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            testData.put(resourceId, 100);
            return true;
        });

        // 验证结果
        assertTrue("锁执行应成功", result);
        assertEquals("资源值应已更新", Integer.valueOf(100), testData.get(resourceId));
    }

    @Test
    public void testLockWithCustomParameters() {
        // 准备测试数据
        String resourceId = "custom-params-" + UUID.randomUUID().toString();
        String lockKey = "test:template:custom:" + resourceId;

        // 执行带自定义参数的锁
        Integer result = lockTemplate.lock(
                lockKey, // 锁键
                10000, // 过期时间（毫秒）
                2, // 重试次数
                300, // 重试间隔（毫秒）
                () -> {
                    // 模拟业务处理
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return 200;
                });

        // 验证结果
        assertEquals("应返回正确的结果", Integer.valueOf(200), result);
    }

    @Test(expected = LockException.class)
    public void testLockFailure() {
        // 准备测试数据
        String lockKey = "test:template:fail:" + UUID.randomUUID().toString();

        // 先获取一个锁
        LockInfo firstLock = lockTemplate.tryLock(lockKey);
        assertNotNull("应成功获取第一个锁", firstLock);

        try {
            // 尝试使用模板再次获取同一个锁，应该抛出异常
            lockTemplate.lock(lockKey, () -> true);
            fail("应该抛出获取锁失败异常");
        } finally {
            // 释放第一个锁
            lockTemplate.unlock(firstLock);
        }
    }

    @Test
    public void testManualLockAndUnlock() {
        // 准备测试数据
        String resourceId = "manual-lock-" + UUID.randomUUID().toString();
        String lockKey = "test:template:manual:" + resourceId;

        // 手动获取锁
        LockInfo lockInfo = lockTemplate.tryLock(lockKey);
        assertNotNull("应成功获取锁", lockInfo);

        try {
            // 模拟业务处理
            testData.put(resourceId, 300);
        } finally {
            // 手动释放锁
            boolean unlocked = lockTemplate.unlock(lockInfo);
            assertTrue("应成功释放锁", unlocked);
        }

        // 验证结果
        assertEquals("资源值应已更新", Integer.valueOf(300), testData.get(resourceId));
    }

    @Test
    public void testTryLockWithParameters() {
        // 准备测试数据
        String resourceId = "try-lock-params-" + UUID.randomUUID().toString();
        String lockKey = "test:template:try:" + resourceId;

        // 尝试获取锁并指定参数
        LockInfo lockInfo = lockTemplate.tryLock(
                lockKey, // 锁键
                5000, // 过期时间（毫秒）
                2, // 重试次数
                200 // 重试间隔（毫秒）
        );

        assertNotNull("应成功获取锁", lockInfo);
        assertEquals("锁键应正确", lockKey, lockInfo.getKey());

        // 释放锁
        boolean unlocked = lockTemplate.unlock(lockInfo);
        assertTrue("应成功释放锁", unlocked);
    }

    @Test
    public void testUnlockWithNullLockInfo() {
        // 释放空锁信息应返回false
        boolean result = lockTemplate.unlock(null);
        assertFalse("释放空锁应返回false", result);
    }

    @Test
    public void testConcurrentLockExecution() throws InterruptedException {
        // 准备测试数据
        final String resourceId = "concurrent-" + UUID.randomUUID().toString();
        final String lockKey = "test:template:concurrent:" + resourceId;
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 启动多个线程并发获取锁
        for (int i = 0; i < threadCount; i++) {
            final int value = i + 1;
            executor.submit(() -> {
                try {
                    try {
                        boolean success = lockTemplate.lock(lockKey, () -> {
                            // 记录第一个成功获取锁的线程值
                            if (!testData.containsKey(resourceId)) {
                                testData.put(resourceId, value);
                                return true;
                            }
                            return false;
                        });

                        if (success) {
                            successCount.incrementAndGet();
                        }
                    } catch (LockException e) {
                        // 忽略锁获取失败异常
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程执行完毕
        latch.await();
        executor.shutdown();

        // 只有一个线程应该成功
        assertEquals("只应有一个线程成功", 1, successCount.get());
        assertNotNull("资源值应已设置", testData.get(resourceId));
        assertTrue("资源值应为1到10之间", testData.get(resourceId) >= 1 && testData.get(resourceId) <= 10);
    }
}