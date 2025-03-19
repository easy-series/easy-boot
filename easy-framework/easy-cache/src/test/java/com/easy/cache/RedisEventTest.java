package com.easy.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheManager;
import com.easy.cache.implementation.DefaultCacheManager;
import com.easy.cache.implementation.remote.RedisRemoteCache;
import com.easy.cache.serialization.JsonSerializer;
import com.easy.cache.serialization.Serializer;
import com.easy.cache.sync.redis.RedisEventPublisher;
import com.easy.cache.sync.redis.RedisEventSubscriber;

/**
 * Redis事件测试
 * 测试跨缓存实例的事件通知机制
 * 使用Redisson客户端连接Redis
 */
@SpringBootTest
public class RedisEventTest {

    @SpringBootConfiguration
    static class TestConfig {

        /**
         * 配置Redisson客户端
         */
        @Bean
        public RedissonClient redissonClient() {
            Config config = new Config();
            // 单节点模式配置
            config.useSingleServer()
                    .setAddress("redis://localhost:6379")
                    .setDatabase(0)
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(2)
                    .setPassword("123456");
            return Redisson.create(config);
        }

        /**
         * 配置Redisson连接工厂
         */
        @Bean
        public RedisConnectionFactory redisConnectionFactory(RedissonClient redissonClient) {
            return new RedissonConnectionFactory(redissonClient);
        }

        /**
         * 配置RedisTemplate
         */
        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(RedisSerializer.json());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(RedisSerializer.json());
            template.afterPropertiesSet();
            return template;
        }

        /**
         * 配置Redis消息监听容器
         */
        @Bean
        public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            return container;
        }

        /**
         * 配置序列化器
         */
        @Bean
        public Serializer serializer() {
            return new JsonSerializer();
        }
    }

    private RedissonClient redissonClient;
    private RedisConnectionFactory redisConnectionFactory;
    private RedisTemplate<String, Object> redisTemplate;
    private RedisMessageListenerContainer listenerContainer;
    private Serializer serializer;
    private RedisEventPublisher eventPublisher;
    private RedisEventSubscriber eventSubscriber;
    private CacheManager cacheManager1;
    private CacheManager cacheManager2;

    @BeforeEach
    public void setUp() {
        // 创建Redisson客户端
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setDatabase(0)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(2)
                .setPassword("123456");
        redissonClient = Redisson.create(config);

        // 创建Redisson连接工厂
        redisConnectionFactory = new RedissonConnectionFactory(redissonClient);

        // 创建RedisTemplate
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(RedisSerializer.json());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());
        redisTemplate.afterPropertiesSet();

        // 创建消息监听容器
        listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(redisConnectionFactory);
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();

        // 创建序列化器和事件发布/订阅组件
        serializer = new JsonSerializer();
        eventPublisher = new RedisEventPublisher(redisTemplate, serializer);
        cacheManager1 = new DefaultCacheManager();
        cacheManager2 = new DefaultCacheManager();
        eventSubscriber = new RedisEventSubscriber(listenerContainer, serializer, cacheManager2);

        // 清空Redis
        // redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    public void testCacheEventPropagation() throws InterruptedException {
        // 准备：通过不同的管理器创建两个缓存 (模拟不同实例)
        CacheConfig config = CacheConfig.builder()
                .remoteExpireSeconds(300)
                .build();

        String cacheName = "testEventCache";

        // 创建第一个缓存
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Cache<String, String> cache1 = new RedisRemoteCache(
                cacheName,
                config,
                redisTemplate,
                serializer,
                eventPublisher,
                null);

        // 创建第二个缓存 (不同的缓存实例)
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Cache<String, String> cache2 = new RedisRemoteCache(
                cacheName,
                config,
                redisTemplate,
                serializer,
                null,
                eventSubscriber);

        // 订阅第二个缓存的事件
        eventSubscriber.subscribe(cacheName);

        // 设置同步点
        CountDownLatch latch = new CountDownLatch(1);

        // 在第一个缓存中放入数据，应该通过事件传播到第二个缓存
        cache1.put("testKey", "testValue");

        // 等待事件传播 (Redis pub/sub可能有延迟)
        Thread.sleep(1000);

        // 验证第二个缓存是否收到了更新
        assertEquals("testValue", cache2.get("testKey"), "事件传播应该更新第二个缓存");

        // 从第一个缓存删除数据，验证删除事件传播
        cache1.remove("testKey");

        // 等待事件传播
        Thread.sleep(1000);

        // 验证第二个缓存是否也删除了数据
        assertNull(cache2.get("testKey"), "删除事件应该也传播到第二个缓存");

        // 测试清空缓存事件
        // 先在两个缓存中都添加数据
        cache1.put("key1", "value1");
        cache1.put("key2", "value2");

        // 等待事件传播到第二个缓存
        Thread.sleep(1000);

        // 验证第二个缓存有数据
        assertTrue(cache2.containsKey("key1"), "第二个缓存应该通过事件获得数据");

        // 清空第一个缓存
        cache1.clear();

        // 等待清空事件传播
        Thread.sleep(1000);

        // 验证第二个缓存也被清空
        assertNull(cache2.get("key1"), "清空事件应该传播到第二个缓存");
        assertNull(cache2.get("key2"), "清空事件应该传播到第二个缓存");
    }

    @Test
    public void testManualEventPublishSubscribe() throws InterruptedException {
        // 测试手动发布和订阅事件
        final String cacheName = "manualEventCache";
        final String testKey = "manualKey";
        final String testValue = "manualValue";

        // 创建缓存实例
        CacheConfig config = CacheConfig.builder().build();

        // 注册订阅
        eventSubscriber.subscribe(cacheName);

        // 获取第二个缓存管理器的缓存实例
        Cache<String, String> cache2 = cacheManager2.getCache(cacheName);

        // 手动发布更新事件
        eventPublisher.publish(cacheName, testKey, null, testValue);

        // 等待事件处理
        Thread.sleep(1000);

        // 验证事件是否被处理
        assertEquals(testValue, cache2.get(testKey), "手动发布的事件应该被处理");

        // 测试删除事件
        eventPublisher.publish(cacheName, testKey, testValue, null);

        // 等待事件处理
        Thread.sleep(1000);

        // 验证删除是否生效
        assertNull(cache2.get(testKey), "删除事件应该被处理");
    }

    /**
     * 测试清理方法
     */
    @AfterEach
    public void tearDown() {
        // 关闭Redisson客户端
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
        }

        // 关闭监听容器
        if (listenerContainer != null) {
            listenerContainer.stop();
        }
    }
}