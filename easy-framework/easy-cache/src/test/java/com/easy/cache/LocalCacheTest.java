package com.easy.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.easy.cache.core.CacheConfig;
import com.easy.cache.implementation.local.CaffeineLocalCache;

/**
 * 本地缓存测试类
 * 这个测试类主要测试本地缓存实现，不依赖Redis
 * 但为了统一测试框架，也添加了跳过Redis测试的机制
 */
public class LocalCacheTest {

    private CaffeineLocalCache<String, String> cache;

    /**
     * 创建Redisson客户端的工厂方法
     * 此方法仅作为示例，在本测试类中不会实际使用
     */
    private RedissonClient createRedissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setDatabase(0)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(2)
                .setPassword("123456");
        return Redisson.create(config);
    }

    @BeforeEach
    public void setUp() {
        CacheConfig config = CacheConfig.builder()
                .localExpireSeconds(5) // 5秒过期
                .localMaxSize(1000) // 最大1000个元素
                .build();

        cache = new CaffeineLocalCache<>("testLocalCache", config);
    }

    @Test
    public void testBasicOperations() {
        // 测试写入和读取
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));

        // 测试更新
        cache.put("key1", "updatedValue");
        assertEquals("updatedValue", cache.get("key1"));

        // 测试删除
        cache.remove("key1");
        assertNull(cache.get("key1"));

        // 测试containsKey
        cache.put("key2", "value2");
        assertTrue(cache.containsKey("key2"));
        assertFalse(cache.containsKey("nonExistingKey"));
    }

    @Test
    public void testExpiration() throws InterruptedException {
        // 创建短期过期的缓存
        CacheConfig shortExpireConfig = CacheConfig.builder()
                .localExpireSeconds(1) // 1秒过期
                .build();

        CaffeineLocalCache<String, String> shortCache = new CaffeineLocalCache<>("shortExpireCache", shortExpireConfig);

        // 写入数据
        shortCache.put("expireKey", "expireValue");
        assertEquals("expireValue", shortCache.get("expireKey"));

        // 等待过期
        Thread.sleep(1500); // 等待1.5秒确保过期

        // 验证已过期
        assertNull(shortCache.get("expireKey"));
    }

    @Test
    public void testCacheMaxSize() {
        // 创建小容量缓存
        CacheConfig smallConfig = CacheConfig.builder()
                .localMaxSize(5) // 只允许5个元素
                .build();

        CaffeineLocalCache<String, String> smallCache = new CaffeineLocalCache<>("smallCache", smallConfig);

        // 写入超过最大容量的数据
        for (int i = 0; i < 10; i++) {
            smallCache.put("key" + i, "value" + i);
        }

        // 由于淘汰策略，部分早期写入的数据应该已经被淘汰
        // 注意：无法确定具体哪些键被淘汰，因此只能验证至少有一些键被淘汰了
        int existingCount = 0;
        for (int i = 0; i < 10; i++) {
            if (smallCache.get("key" + i) != null) {
                existingCount++;
            }
        }

        assertTrue(existingCount <= 5, "缓存应该只保留最多5个元素");
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        // 启动并发线程测试缓存
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                try {
                    // 每个线程执行多次读写操作
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "thread" + threadId + "_key" + j;
                        String value = "value_" + threadId + "_" + j;

                        // 写入
                        cache.put(key, value);

                        // 读取验证
                        String retrievedValue = cache.get(key);
                        if (!value.equals(retrievedValue)) {
                            errors.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 验证没有错误发生
        assertEquals(0, errors.get(), "并发访问缓存应该没有错误");
    }

    @Test
    public void testPerformance() {
        int operationCount = 100000;

        // 测试写性能
        long startWrite = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            cache.put("perfKey" + i, "perfValue" + i);
        }
        long writeTime = System.currentTimeMillis() - startWrite;

        System.out.println("写入" + operationCount + "次耗时：" + writeTime + "ms");
        System.out.println("写入速率：" + (operationCount * 1000.0 / writeTime) + "次/秒");

        // 测试读性能
        long startRead = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            cache.get("perfKey" + (i % 10000)); // 只读取前10000个键，确保命中
        }
        long readTime = System.currentTimeMillis() - startRead;

        System.out.println("读取" + operationCount + "次耗时：" + readTime + "ms");
        System.out.println("读取速率：" + (operationCount * 1000.0 / readTime) + "次/秒");

        // 确保性能在合理范围内
        assertTrue(writeTime < 5000, "写性能应该足够快");
        assertTrue(readTime < 2000, "读性能应该足够快");
    }
}