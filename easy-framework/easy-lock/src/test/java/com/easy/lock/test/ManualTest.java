package com.easy.lock.test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.easy.lock.core.LockInfo;
import com.easy.lock.core.RedisLock;
import com.easy.lock.core.executor.RedisLockExecutor;
import com.easy.lock.template.LockTemplate;

/**
 * 手动测试类，用于直接测试分布式锁功能
 * 这个类可以独立运行，不依赖Spring环境
 */
public class ManualTest {

    public static void main(String[] args) {
        System.out.println("====== 开始分布式锁测试 ======");

        // 创建Redis连接
        RedissonConnectionFactory connectionFactory = null;
        StringRedisTemplate redisTemplate = null;
        RedissonClient redissonClient = null;

        try {
            // 创建Redisson配置
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://localhost:6379")
                    .setPassword("123456")
                    .setDatabase(0);

            // 创建Redisson客户端
            redissonClient = Redisson.create(config);

            // 创建Redis连接工厂
            connectionFactory = new RedissonConnectionFactory(redissonClient);

            // 创建Redis模板
            redisTemplate = new StringRedisTemplate();
            redisTemplate.setConnectionFactory(connectionFactory);
            redisTemplate.afterPropertiesSet();

            // 测试基本锁功能
            testBasicLock(redisTemplate);

            // 测试锁模板
            testLockTemplate(redisTemplate);

            // 测试并发锁
            testConcurrentLock(redisTemplate);

            // 测试锁的性能
            benchmarkLockPerformance(redisTemplate);

        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (redissonClient != null) {
                redissonClient.shutdown();
                System.out.println("已关闭Redis连接");
            }
        }

        System.out.println("====== 分布式锁测试完成 ======");
    }

    /**
     * 测试基本锁功能
     */
    private static void testBasicLock(StringRedisTemplate redisTemplate) {
        System.out.println("\n===== 测试基本锁功能 =====");

        // 创建Redis锁执行器
        RedisLockExecutor lockExecutor = new RedisLockExecutor(redisTemplate);

        // 创建Redis锁
        RedisLock lock = new RedisLock(lockExecutor);

        // 锁的键和值
        String key = "test:manual:basic";
        long expireTime = 10000; // 10秒
        int retryCount = 3;
        long retryInterval = 100;

        // 尝试获取锁
        System.out.println("尝试获取锁: " + key);
        LockInfo lockInfo = lock.tryLock(key, expireTime, retryCount, retryInterval);

        if (lockInfo != null) {
            System.out.println("成功获取锁: " + lockInfo);
            System.out.println("锁键: " + lockInfo.getKey());
            System.out.println("锁值: " + lockInfo.getValue());
            System.out.println("锁类型: " + lockInfo.getType());
            System.out.println("锁状态: " + lockInfo.getState());
            System.out.println("过期时间: " + lockInfo.getExpireTime() + "ms");
            System.out.println("获取时间: " + lockInfo.getAcquireTime());

            // 检查锁是否被占用
            boolean isLocked = lock.isLocked(key);
            System.out.println("锁是否被占用: " + isLocked);

            // 释放锁
            boolean released = lock.releaseLock(lockInfo);
            System.out.println("释放锁结果: " + released);

            // 再次检查锁是否被占用
            isLocked = lock.isLocked(key);
            System.out.println("释放后锁是否被占用: " + isLocked);
        } else {
            System.out.println("获取锁失败");
        }
    }

    /**
     * 测试锁模板
     */
    private static void testLockTemplate(StringRedisTemplate redisTemplate) {
        System.out.println("\n===== 测试锁模板 =====");

        // 创建Redis锁执行器
        RedisLockExecutor lockExecutor = new RedisLockExecutor(redisTemplate);

        // 创建Redis锁
        RedisLock lock = new RedisLock(lockExecutor);

        // 创建锁模板
        LockTemplate lockTemplate = new LockTemplate(lock);

        // 使用锁模板执行操作
        String key = "test:manual:template";
        String result = lockTemplate.lock(key, () -> {
            System.out.println("在锁保护下执行操作");
            // 模拟业务处理
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "操作成功，时间: " + System.currentTimeMillis();
        });

        System.out.println("锁模板执行结果: " + result);

        // 使用自定义参数
        result = lockTemplate.lock(key, 5000, 2, 200, () -> {
            System.out.println("使用自定义参数在锁保护下执行操作");
            return "自定义参数操作成功";
        });

        System.out.println("自定义参数锁模板执行结果: " + result);
    }

    /**
     * 测试并发锁
     */
    private static void testConcurrentLock(StringRedisTemplate redisTemplate) throws InterruptedException {
        System.out.println("\n===== 测试并发锁 =====");

        // 创建Redis锁执行器
        RedisLockExecutor lockExecutor = new RedisLockExecutor(redisTemplate);

        // 创建Redis锁
        RedisLock lock = new RedisLock(lockExecutor);

        // 创建锁模板
        LockTemplate lockTemplate = new LockTemplate(lock);

        // 共享计数器
        AtomicInteger counter = new AtomicInteger(0);

        // 并发线程数
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 锁的键
        String key = "test:manual:concurrent";

        System.out.println("启动 " + threadCount + " 个线程并发获取锁");

        // 启动多个线程
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // 使用锁模板执行操作
                    Integer result = lockTemplate.lock(key, 30000, 3, 100, () -> {
                        System.out.println("线程 " + index + " 获取到锁");
                        // 模拟业务处理
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        // 增加计数器
                        return counter.incrementAndGet();
                    });

                    System.out.println("线程 " + index + " 完成，计数器值: " + result);
                } catch (Exception e) {
                    System.err.println("线程 " + index + " 发生错误: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await(30, TimeUnit.SECONDS);

        System.out.println("所有线程执行完毕，最终计数器值: " + counter.get());
        if (counter.get() == threadCount) {
            System.out.println("并发锁测试成功，所有线程都正确执行");
        } else {
            System.out.println("并发锁测试失败，计数器值与线程数不一致");
        }

        // 关闭线程池
        executor.shutdown();
    }

    /**
     * 测试锁的性能
     */
    private static void benchmarkLockPerformance(StringRedisTemplate redisTemplate) throws InterruptedException {
        System.out.println("\n===== 测试锁性能 =====");

        // 创建Redis锁执行器
        RedisLockExecutor lockExecutor = new RedisLockExecutor(redisTemplate);

        // 创建Redis锁
        RedisLock lock = new RedisLock(lockExecutor);

        // 创建锁模板
        LockTemplate lockTemplate = new LockTemplate(lock);

        // 测试参数
        int iterations = 1000;
        String keyPrefix = "test:manual:perf:";

        System.out.println("执行 " + iterations + " 次锁操作");

        // 测试获取和释放锁的性能
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            String key = keyPrefix + UUID.randomUUID().toString();
            LockInfo lockInfo = lockTemplate.tryLock(key);
            if (lockInfo != null) {
                lockTemplate.unlock(lockInfo);
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("执行 " + iterations + " 次锁操作耗时: " + duration + "ms");
        System.out.println("平均每次操作耗时: " + (double) duration / iterations + "ms");
        System.out.println("每秒可执行锁操作次数: " + (iterations * 1000L / duration));

        // 测试锁模板的性能
        System.out.println("\n测试锁模板性能");
        startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            String key = keyPrefix + UUID.randomUUID().toString();
            lockTemplate.lock(key, () -> null);
        }

        endTime = System.currentTimeMillis();
        duration = endTime - startTime;

        System.out.println("执行 " + iterations + " 次锁模板操作耗时: " + duration + "ms");
        System.out.println("平均每次操作耗时: " + (double) duration / iterations + "ms");
        System.out.println("每秒可执行锁模板操作次数: " + (iterations * 1000L / duration));
    }
}