package com.easy.lock.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.easy.lock.config.EasyLockAutoConfiguration;
import com.easy.lock.config.EasyLockProperties;
import com.easy.lock.core.Lock;
import com.easy.lock.core.LockInfo;
import com.easy.lock.core.RedisLock;
import com.easy.lock.exception.LockException;
import com.easy.lock.template.LockTemplate;

/**
 * Easy-Lock 自动装配测试类
 * <p>
 * 测试Easy-Lock模块的自动装配功能
 */
@SpringBootTest(classes = EasyLockAutoConfigurationTest.TestConfig.class)
@ActiveProfiles("test")
public class EasyLockAutoConfigurationTest {

    /**
     * 测试配置类，启用自动配置
     */
    @Configuration
    @EnableAutoConfiguration
    @ImportAutoConfiguration({ RedisAutoConfiguration.class, EasyLockAutoConfiguration.class })
    public static class TestConfig {
        // 空配置类，用于引导Spring Boot应用上下文

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
            template.afterPropertiesSet();
            return template;
        }
    }

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private LockTemplate lockTemplate;

    @Autowired(required = false)
    private EasyLockProperties lockProperties;

    @Autowired(required = false)
    private Lock lock;

    /**
     * 测试是否正确自动装配了EasyLockProperties配置类
     */
    @Test
    public void testLockPropertiesAutowired() {
        System.out.println("===== 测试EasyLockProperties自动装配 =====");

        // 打印所有bean名称，帮助诊断
        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("=== 已创建的Bean列表 ===");
        for (String beanName : beanNames) {
            System.out.println(beanName);
        }

        // 断言EasyLockProperties被成功注入
        assertNotNull(lockProperties, "EasyLockProperties 应该被成功注入");

        // 验证配置属性是否正确加载
        System.out.println("是否启用分布式锁: " + lockProperties.isEnabled());
        System.out.println("锁前缀: " + lockProperties.getPrefix());
        System.out.println("默认过期时间: " + lockProperties.getExpireTime() + "ms");
        System.out.println("默认重试次数: " + lockProperties.getRetryCount());
        System.out.println("默认重试间隔: " + lockProperties.getRetryInterval() + "ms");
        System.out.println("是否启用锁监控: " + lockProperties.isMonitorEnabled());
    }

    /**
     * 测试是否正确自动装配了Lock实现类
     */
    @Test
    public void testLockBeanAutowired() {
        System.out.println("===== 测试Lock Bean自动装配 =====");

        // 断言Lock被成功注入
        assertNotNull(lock, "Lock 应该被成功注入");
        assertTrue(lock instanceof RedisLock, "Lock 应该是 RedisLock 类型");

        System.out.println("Lock实现类: " + lock.getClass().getSimpleName());
    }

    /**
     * 测试是否正确自动装配了LockTemplate
     */
    @Test
    public void testLockTemplateAutowired() {
        System.out.println("===== 测试LockTemplate自动装配 =====");

        // 断言LockTemplate被成功注入
        assertNotNull(lockTemplate, "LockTemplate 应该被成功注入");
    }

    /**
     * 测试基本的锁获取和释放功能
     */
    @Test
    public void testBasicLockFunctionality() {
        System.out.println("===== 测试基本锁功能 =====");

        String testKey = "test:lock:basic";

        // 尝试获取锁
        LockInfo lockInfo = lockTemplate.tryLock(testKey);
        assertNotNull(lockInfo, "应该能够获取锁");

        // 验证锁信息
        System.out.println("锁信息: " + lockInfo);
        System.out.println("锁类型: " + lockInfo.getType());
        System.out.println("锁键: " + lockInfo.getKey());
        System.out.println("锁值: " + lockInfo.getValue());

        // 验证锁已被获取
        assertTrue(lock.isLocked(testKey), "锁应该处于锁定状态");

        // 释放锁
        boolean released = lockTemplate.unlock(lockInfo);
        assertTrue(released, "锁应该被成功释放");

        // 验证锁已被释放
        assertFalse(lock.isLocked(testKey), "锁应该已被释放");
    }

    /**
     * 测试锁模板的执行功能
     */
    @Test
    public void testLockTemplateExecution() {
        System.out.println("===== 测试锁模板执行功能 =====");

        String testKey = "test:lock:execution";

        // 使用锁模板执行操作
        String result = lockTemplate.lock(testKey, () -> {
            System.out.println("在锁保护下执行操作");
            return "操作成功";
        });

        System.out.println("执行结果: " + result);
        assertNotNull(result, "执行结果不应为空");

        // 验证锁已被释放
        assertFalse(lock.isLocked(testKey), "操作完成后锁应该已被释放");
    }

    /**
     * 测试锁的并发控制能力
     */
    @Test
    public void testConcurrentLockControl() throws InterruptedException {
        System.out.println("===== 测试锁的并发控制能力 =====");

        String testKey = "test:lock:concurrent";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 创建一个长时间持有的锁
        LockInfo holdingLock = lockTemplate.tryLock(testKey, 10000, 0, 0);
        assertNotNull(holdingLock, "应该能够获取长时间锁");

        // 启动多个线程尝试获取同一个锁
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 尝试获取已被持有的锁，应该失败
                    LockInfo lockInfo = lockTemplate.tryLock(testKey, 1000, 1, 100);
                    if (lockInfo != null) {
                        successCount.incrementAndGet();
                        lockTemplate.unlock(lockInfo);
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await(5, TimeUnit.SECONDS);

        // 释放长时间持有的锁
        lockTemplate.unlock(holdingLock);

        System.out.println("成功获取锁的线程数: " + successCount.get());
        System.out.println("获取锁失败的线程数: " + failureCount.get());

        // 由于锁被长时间持有，所有线程应该都获取锁失败
        assertEquals(0, successCount.get(), "所有线程应该获取锁失败");
        assertEquals(threadCount, failureCount.get(), "所有线程应该获取锁失败");

        // 验证锁已被释放
        assertFalse(lock.isLocked(testKey), "测试结束后锁应该已被释放");

        // 关闭线程池
        executor.shutdown();
    }

    /**
     * 测试锁获取失败时的异常
     */
    @Test
    public void testLockFailureException() {
        System.out.println("===== 测试锁获取失败异常 =====");

        String testKey = "test:lock:failure";

        // 先获取一个锁
        LockInfo firstLock = lockTemplate.tryLock(testKey, 10000, 0, 0);
        assertNotNull(firstLock, "应该能够获取第一个锁");

        try {
            // 使用锁模板执行，应该抛出异常
            assertThrows(LockException.class, () -> {
                lockTemplate.lock(testKey, 1000, 1, 100, () -> {
                    System.out.println("这段代码不应该被执行");
                    return "不应该返回";
                });
            }, "应该抛出LockException");

            System.out.println("成功捕获到锁获取失败异常");
        } finally {
            // 释放第一个锁
            lockTemplate.unlock(firstLock);
        }
    }

    /**
     * 测试在实际业务中使用分布式锁
     */
    @Test
    public void testUsingLockInBusinessService() {
        System.out.println("===== 测试在业务服务中使用分布式锁 =====");

        // 模拟业务服务类注入LockTemplate
        BusinessService businessService = new BusinessService(lockTemplate);

        // 使用业务服务类执行加锁操作
        String result = businessService.executeWithLock("test:business:lock", "业务参数");
        System.out.println("业务执行结果: " + result);
        assertNotNull(result, "业务执行结果不应为空");
    }

    /**
     * 模拟业务服务类
     */
    static class BusinessService {
        private final LockTemplate lockTemplate;

        public BusinessService(LockTemplate lockTemplate) {
            this.lockTemplate = lockTemplate;
        }

        public String executeWithLock(String key, String param) {
            // 使用分布式锁保护业务操作
            return lockTemplate.lock(key, 30000, 3, 100, () -> {
                System.out.println("执行业务逻辑，参数: " + param);
                // 模拟业务处理
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "处理完成: " + param;
            });
        }
    }

    /**
     * 辅助方法：断言相等
     */
    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }
}