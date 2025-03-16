package com.easy.lock.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.easy.lock.BaseLockTest;
import com.easy.lock.core.LockInfo;
import com.easy.lock.core.RedisLock;

/**
 * 锁监控测试类
 */
public class LockMonitorTest extends BaseLockTest {

    @Autowired
    private LockMonitor lockMonitor;

    @Autowired
    private RedisLock redisLock;

    @Before
    public void setUp() {
        // 每个测试前重置监控数据
        lockMonitor.reset();
    }

    @Test
    public void testBasicMonitoring() {
        // 记录成功和失败
        String key1 = "test:monitor:" + UUID.randomUUID().toString();
        lockMonitor.recordSuccess(key1);
        lockMonitor.recordLockTime(key1, 10);

        String key2 = "test:monitor:" + UUID.randomUUID().toString();
        lockMonitor.recordFail(key2);

        // 验证计数
        assertEquals("成功计数应为1", 1, lockMonitor.getSuccessCount().get());
        assertEquals("失败计数应为1", 1, lockMonitor.getFailCount().get());
        assertEquals("总锁定次数应为1", 1, lockMonitor.getTotalLockCount().get());
        assertEquals("总锁定时间应为10", 10, lockMonitor.getTotalLockTime().get());
        assertEquals("锁失败比例应为0.5", 0.5, lockMonitor.getFailRate(), 0.001);
        assertEquals("平均锁定时间应为10", 10.0, lockMonitor.getAverageLockTime(), 0.001);
    }

    @Test
    public void testRealLockMonitoring() {
        // 执行成功的锁操作
        String lockKey = "test:monitor:real:" + UUID.randomUUID().toString();
        LockInfo lockInfo = redisLock.tryLock(lockKey, 10000, 0, 0);
        assertNotNull("应该成功获取锁", lockInfo);

        // 模拟保持锁一段时间
        try {
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 释放锁
        boolean released = redisLock.releaseLock(lockInfo);
        assertTrue("应该成功释放锁", released);

        // 执行失败的锁操作
        String conflictKey = "test:monitor:conflict:" + UUID.randomUUID().toString();
        LockInfo firstLock = redisLock.tryLock(conflictKey, 10000, 0, 0);
        assertNotNull("应该成功获取第一个锁", firstLock);

        // 尝试再次获取同一个锁，应该失败
        LockInfo secondLock = redisLock.tryLock(conflictKey, 10000, 0, 0);
        assertNull("应该获取锁失败", secondLock);

        // 释放第一个锁
        redisLock.releaseLock(firstLock);

        // 验证监控数据
        assertTrue("成功计数应大于0", lockMonitor.getSuccessCount().get() > 0);
        assertTrue("失败计数应大于0", lockMonitor.getFailCount().get() > 0);
        assertTrue("平均锁定时间应大于0", lockMonitor.getAverageLockTime() > 0);
    }

    @Test
    public void testResourceLevelMonitoring() {
        // 创建两个不同的资源锁
        String resource1 = "test:monitor:resource1:" + UUID.randomUUID().toString();
        String resource2 = "test:monitor:resource2:" + UUID.randomUUID().toString();

        // 对resource1记录成功和失败
        lockMonitor.recordSuccess(resource1);
        lockMonitor.recordSuccess(resource1);
        lockMonitor.recordFail(resource1);

        // 对resource2只记录成功
        lockMonitor.recordSuccess(resource2);
        lockMonitor.recordSuccess(resource2);

        // 验证资源统计
        assertEquals("resource1成功计数应为2", 2, lockMonitor.getResourceSuccessCount(resource1));
        assertEquals("resource1失败计数应为1", 1, lockMonitor.getResourceFailCount(resource1));
        assertEquals("resource1争用比例应为1/3", 1.0 / 3.0, lockMonitor.getResourceContentionRate(resource1), 0.001);

        assertEquals("resource2成功计数应为2", 2, lockMonitor.getResourceSuccessCount(resource2));
        assertEquals("resource2失败计数应为0", 0, lockMonitor.getResourceFailCount(resource2));
        assertEquals("resource2争用比例应为0", 0.0, lockMonitor.getResourceContentionRate(resource2), 0.001);

        // 检查资源级别统计
        Map<String, LockMonitor.ResourceStats> resourceStats = lockMonitor.getResourceStats();
        assertNotNull("资源统计不应为空", resourceStats);
        assertEquals("应该有2个资源", 2, resourceStats.size());

        // 验证resource1的统计
        assertTrue("应该包含resource1的统计", resourceStats.containsKey(resource1));
        LockMonitor.ResourceStats stats1 = resourceStats.get(resource1);
        assertEquals("resource1成功次数应为2", 2, stats1.getSuccessCount());
        assertEquals("resource1失败次数应为1", 1, stats1.getFailCount());
        assertEquals("resource1争用率应为1/3", 1.0 / 3.0, stats1.getContentionRate(), 0.001);

        // 验证resource2的统计
        assertTrue("应该包含resource2的统计", resourceStats.containsKey(resource2));
        LockMonitor.ResourceStats stats2 = resourceStats.get(resource2);
        assertEquals("resource2成功次数应为2", 2, stats2.getSuccessCount());
        assertEquals("resource2失败次数应为0", 0, stats2.getFailCount());
        assertEquals("resource2争用率应为0", 0.0, stats2.getContentionRate(), 0.001);
    }

    @Test
    public void testMaxLockTimeRecord() {
        String key1 = "test:monitor:max1:" + UUID.randomUUID().toString();
        lockMonitor.recordSuccess(key1);
        lockMonitor.recordLockTime(key1, 100);

        String key2 = "test:monitor:max2:" + UUID.randomUUID().toString();
        lockMonitor.recordSuccess(key2);
        lockMonitor.recordLockTime(key2, 200);

        String key3 = "test:monitor:max3:" + UUID.randomUUID().toString();
        lockMonitor.recordSuccess(key3);
        lockMonitor.recordLockTime(key3, 50);

        // 验证最大锁定时间记录
        LockMonitor.LockRecord maxRecord = lockMonitor.getMaxTimeRecord();
        assertNotNull("最大锁定时间记录不应为空", maxRecord);
        assertEquals("最大锁定时间应为200", 200, maxRecord.getLockTime());
        assertEquals("最大锁定时间的键应为key2", key2, maxRecord.getKey());
    }

    @Test
    public void testConcurrentMonitoring() throws InterruptedException {
        final String lockKey = "test:monitor:concurrent:" + UUID.randomUUID().toString();
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 启动多个线程并发获取锁
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    LockInfo lockInfo = redisLock.tryLock(lockKey, 30000, 0, 0);
                    if (lockInfo != null) {
                        try {
                            // 模拟业务处理
                            Thread.sleep(10);
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

        // 验证监控指标
        assertEquals("总的成功和失败次数应为线程数量", threadCount,
                lockMonitor.getSuccessCount().get() + lockMonitor.getFailCount().get());
        assertTrue("应该有成功的锁操作", lockMonitor.getSuccessCount().get() > 0);
        assertTrue("应该有失败的锁操作", lockMonitor.getFailCount().get() > 0);
        assertTrue("失败计数应接近线程数-1", Math.abs(lockMonitor.getFailCount().get() - (threadCount - 1)) <= 1);
    }

    @Test
    public void testResetMonitor() {
        // 首先记录一些数据
        lockMonitor.recordSuccess("key1");
        lockMonitor.recordFail("key2");
        lockMonitor.recordLockTime("key1", 100);

        // 验证数据已记录
        assertTrue("成功计数应大于0", lockMonitor.getSuccessCount().get() > 0);
        assertTrue("失败计数应大于0", lockMonitor.getFailCount().get() > 0);

        // 重置监控数据
        lockMonitor.reset();

        // 验证数据已重置
        assertEquals("成功计数应为0", 0, lockMonitor.getSuccessCount().get());
        assertEquals("失败计数应为0", 0, lockMonitor.getFailCount().get());
        assertEquals("总锁定时间应为0", 0, lockMonitor.getTotalLockTime().get());
        assertNull("最大锁定时间记录应为null", lockMonitor.getMaxTimeRecord());
        assertTrue("资源统计应为空", lockMonitor.getResourceStats().isEmpty());
    }
}