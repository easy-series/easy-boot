package com.easy.lock.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.easy.lock.annotation.EasyLock;
import com.easy.lock.core.Lock;
import com.easy.lock.core.RedisLock;
import com.easy.lock.core.executor.RedisLockExecutor;
import com.easy.lock.exception.LockException;
import com.easy.lock.template.LockTemplate;

/**
 * Easy-Lock 注解测试类
 * <p>
 * 测试Easy-Lock注解的功能
 */
@SpringBootTest(classes = EasyLockAnnotationTest.TestConfig.class)
@ActiveProfiles("test")
public class EasyLockAnnotationTest {

    /**
     * 测试配置类，启用自动配置和AOP
     */
    @Configuration
    @EnableAutoConfiguration
    @EnableAspectJAutoProxy
    public static class TestConfig {

        @Bean
        public TestService testService() {
            return new TestService();
        }

        @Bean
        public TestBusinessService testBusinessService() {
            return new TestBusinessService();
        }

        // 配置Redisson客户端，确保使用Redisson而不是Lettuce
        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://localhost:6379")
                    .setPassword("123456")
                    .setDatabase(0);

            RedissonClient redisson = Redisson.create(config);
            return new RedissonConnectionFactory(redisson);
        }

        // 添加StringRedisTemplate Bean，因为RedisLockExecutor需要它
        @Bean
        public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
            StringRedisTemplate template = new StringRedisTemplate();
            template.setConnectionFactory(redisConnectionFactory);
            return template;
        }

        // 手动配置锁相关的Bean，确保它们被正确创建
        @Bean
        public RedisLockExecutor redisLockExecutor(StringRedisTemplate redisTemplate) {
            return new RedisLockExecutor(redisTemplate);
        }

        @Bean
        public RedisLock redisLock(RedisLockExecutor redisLockExecutor) {
            return new RedisLock(redisLockExecutor);
        }

        @Bean
        public LockTemplate lockTemplate(Lock lock) {
            return new LockTemplate(lock);
        }
    }

    @Autowired
    private TestService testService;

    @Autowired
    private TestBusinessService testBusinessService;

    /**
     * 测试基本的锁注解功能
     */
    @Test
    public void testBasicLockAnnotation() {
        System.out.println("===== 测试基本锁注解功能 =====");

        // 调用带有锁注解的方法
        String result = testService.doSomethingWithLock("test-param");

        // 验证结果
        assertNotNull(result, "方法执行结果不应为空");
        assertEquals("处理完成: test-param", result, "方法执行结果不正确");
    }

    /**
     * 测试锁注解的并发控制能力
     */
    @Test
    public void testConcurrentLockAnnotation() throws InterruptedException {
        System.out.println("===== 测试锁注解的并发控制能力 =====");

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger counter = new AtomicInteger(0);

        // 启动多个线程并发调用带锁的方法
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // 添加随机延迟，避免所有线程同时获取锁
                    Thread.sleep(50 * index);
                    // 调用会增加计数器的方法
                    testService.incrementCounter(counter, 100);
                    System.out.println("线程 " + index + " 完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成，增加等待时间
        latch.await(30, TimeUnit.SECONDS);

        // 验证计数器值
        System.out.println("最终计数器值: " + counter.get());
        assertEquals(threadCount, counter.get(), "计数器值应该等于线程数");

        // 关闭线程池
        executor.shutdown();
    }

    /**
     * 测试锁注解的失败策略
     */
    @Test
    public void testLockFailureStrategy() {
        System.out.println("===== 测试锁注解的失败策略 =====");

        // 先调用一个长时间持有锁的方法
        Thread holdingThread = new Thread(() -> {
            testService.holdLockForLongTime(5000);
        });
        holdingThread.start();

        // 等待一会儿确保锁被获取
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 尝试获取同一个锁，应该抛出异常
        assertThrows(LockException.class, () -> {
            testService.doSomethingWithSameLock("test");
        }, "应该抛出LockException");

        System.out.println("成功捕获到锁获取失败异常");

        // 尝试获取同一个锁，但使用IGNORE策略，应该不抛出异常
        String result = testService.doSomethingWithSameLockIgnoreFailure("test");
        assertNotNull(result, "使用IGNORE策略时不应抛出异常");
        assertEquals("操作被忽略", result, "使用IGNORE策略时应返回特定值");

        // 等待持有锁的线程完成
        try {
            holdingThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 测试在实际业务场景中使用锁注解
     */
    @Test
    public void testLockAnnotationInBusinessScenario() {
        System.out.println("===== 测试在业务场景中使用锁注解 =====");

        // 模拟订单处理
        String orderId = "ORD-" + System.currentTimeMillis();
        String result = testBusinessService.processOrder(orderId, "商品A", 2);

        assertNotNull(result, "订单处理结果不应为空");
        System.out.println("订单处理结果: " + result);
    }

    /**
     * 测试服务类，包含带有锁注解的方法
     */
    public static class TestService {

        /**
         * 使用分布式锁保护的方法
         */
        @EasyLock(key = "'test:lock:' + #param", expire = 30000)
        public String doSomethingWithLock(String param) {
            System.out.println("执行带锁的方法，参数: " + param);
            // 模拟业务处理
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "处理完成: " + param;
        }

        /**
         * 增加计数器的方法，使用分布式锁保护
         */
        @EasyLock(key = "'test:lock:counter'", expire = 30000, retryCount = 10, retryInterval = 200)
        public void incrementCounter(AtomicInteger counter, long sleepTime) {
            System.out.println("线程 " + Thread.currentThread().getId() + " 开始增加计数器");
            // 模拟耗时操作
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            counter.incrementAndGet();
            System.out.println("线程 " + Thread.currentThread().getId() + " 完成增加计数器");
        }

        /**
         * 长时间持有锁的方法
         */
        @EasyLock(key = "'test:lock:long-hold'", expire = 30000)
        public void holdLockForLongTime(long holdTime) {
            System.out.println("获取长时间锁，持有时间: " + holdTime + "ms");
            try {
                Thread.sleep(holdTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("释放长时间锁");
        }

        /**
         * 尝试获取同一个锁的方法，默认失败策略（抛出异常）
         */
        @EasyLock(key = "'test:lock:long-hold'", expire = 5000, retryCount = 1, retryInterval = 100)
        public String doSomethingWithSameLock(String param) {
            System.out.println("这段代码不应该被执行，因为锁已被占用");
            return "不应该返回";
        }

        /**
         * 尝试获取同一个锁的方法，使用IGNORE失败策略
         */
        @EasyLock(key = "'test:lock:long-hold'", expire = 5000, retryCount = 1, retryInterval = 100, failStrategy = EasyLock.FailStrategy.IGNORE)
        public String doSomethingWithSameLockIgnoreFailure(String param) {
            System.out.println("使用IGNORE策略，锁获取失败时会继续执行");
            return "操作被忽略";
        }
    }

    /**
     * 模拟业务服务类
     */
    public static class TestBusinessService {

        /**
         * 处理订单的方法，使用订单ID作为锁的键
         */
        @EasyLock(key = "'order:' + #orderId", expire = 30000)
        public String processOrder(String orderId, String productName, int quantity) {
            System.out.println("处理订单: " + orderId);
            System.out.println("商品: " + productName + ", 数量: " + quantity);

            // 模拟订单处理
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return "订单 " + orderId + " 处理成功，处理时间: " + System.currentTimeMillis();
        }
    }
}