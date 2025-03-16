package com.easy.lock.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.easy.lock.BaseLockTest;
import com.easy.lock.core.LockInfo;
import com.easy.lock.core.RedisLock;
import com.easy.lock.monitor.LockMonitor;

/**
 * 分布式锁注解测试类
 */
public class EasyLockAnnotationTest extends BaseLockTest {

    @Autowired
    private TestService testService;

    @Autowired
    private LockMonitor lockMonitor;

    @Autowired
    private RedisLock redisLock;

    @Test
    public void testBasicAnnotationLock() {
        // 清空监控数据
        lockMonitor.reset();

        Long userId = 1000L;
        String name = "测试用户";

        // 调用加锁方法
        testService.updateUser(userId, name);

        // 验证方法执行成功
        assertEquals("用户名应已更新", name, testService.getUserName(userId));

        // 验证锁监控数据
        assertTrue("应有成功锁请求", lockMonitor.getSuccessCount().get() > 0);
        assertEquals("应有0次失败锁请求", 0, lockMonitor.getFailCount().get());
    }

    @Test
    public void testConcurrentAnnotationLock() throws InterruptedException {
        // 清空监控数据
        lockMonitor.reset();

        final String productId = UUID.randomUUID().toString();
        final int threadCount = 5;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 启动多个线程并发修改库存
        for (int i = 0; i < threadCount; i++) {
            final int quantity = i + 1;
            executor.submit(() -> {
                try {
                    boolean success = testService.updateStock(productId, quantity);
                    if (success) {
                        successCount.incrementAndGet();
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
        assertEquals("只应有一个线程成功更新库存", 1, successCount.get());

        // 验证锁监控数据
        assertTrue("应有成功锁请求", lockMonitor.getSuccessCount().get() > 0);
        assertTrue("应有失败锁请求", lockMonitor.getFailCount().get() > 0);
    }

    @Test
    public void testSpELExpressionLock() {
        // 清空监控数据
        lockMonitor.reset();

        TestOrder order = new TestOrder();
        order.setOrderNo("ORDER-" + UUID.randomUUID().toString().substring(0, 8));
        order.setAmount(100.0);

        // 调用使用SpEL表达式的锁方法
        boolean success = testService.processOrder(order);

        // 验证方法执行成功
        assertTrue("订单处理应成功", success);

        // 验证锁监控数据
        assertTrue("应有成功锁请求", lockMonitor.getSuccessCount().get() > 0);
    }

    @Test
    public void testRetryAspect() throws InterruptedException {
        // 清空监控数据
        lockMonitor.reset();

        final String resourceId = "retry-test-" + UUID.randomUUID().toString();

        // 先获取一个锁，使第一次尝试失败
        final String lockKey = "resource:" + resourceId;
        LockInfo lockInfo = redisLock.tryLock(lockKey, 5000, 0, 0);
        assertNotNull("预先获取锁应成功", lockInfo);

        // 启动新线程执行带重试的方法
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger resultHolder = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                // 执行带重试的方法，此方法会重试3次
                boolean result = testService.accessResourceWithRetry(resourceId);
                if (result) {
                    resultHolder.set(1);
                }
            } finally {
                latch.countDown();
            }
        });

        // 等待一段时间后释放锁，让重试能成功
        Thread.sleep(500);
        redisLock.releaseLock(lockInfo);

        // 等待线程完成
        latch.await();
        executor.shutdown();

        // 验证方法最终执行成功（通过重试）
        assertEquals("方法最终应通过重试成功", 1, resultHolder.get());
    }

    @Test
    public void testCustomFailStrategyIgnore() {
        // 清空监控数据
        lockMonitor.reset();

        String resourceId = "ignore-strategy-" + UUID.randomUUID().toString();

        // 先获取一个锁
        LockInfo lockInfo = redisLock.tryLock("custom:" + resourceId, 10000, 0, 0);
        assertNotNull("预先获取锁应成功", lockInfo);

        try {
            // 使用忽略策略，应该不抛异常，返回true
            boolean result = testService.customFailStrategy(resourceId, EasyLock.FailStrategy.IGNORE);
            assertTrue("应该返回true", result);
        } finally {
            redisLock.releaseLock(lockInfo);
        }
    }

    /**
     * 测试用订单类
     */
    public static class TestOrder {
        private String orderNo;
        private double amount;

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }
    }

    /**
     * 测试服务配置类
     */
    @Configuration
    public static class TestConfig {

        @Bean
        public TestService testService() {
            return new TestService();
        }
    }
}