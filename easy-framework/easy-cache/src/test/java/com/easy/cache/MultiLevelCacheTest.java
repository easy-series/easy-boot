package com.easy.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.implementation.SimpleMultiLevelCache;
import com.easy.cache.implementation.local.CaffeineLocalCache;
import com.easy.cache.implementation.remote.RedisRemoteCache;
import com.easy.cache.serialization.JsonSerializer;
import com.easy.cache.serialization.Serializer;
import com.easy.cache.sync.lock.DefaultDistributedLock;
import com.easy.cache.sync.lock.DistributedLock;

/**
 * 多级缓存测试类
 * 验证多级缓存的读写策略、缓存一致性等功能
 * 
 * 运行测试前请确保Redis服务可用
 * 如需跳过Redis相关测试，请使用: mvn test -DskipRedisTests=true
 */
public class MultiLevelCacheTest {

    /**
     * 是否跳过Redis测试的系统属性名
     */

    // 测试使用的缓存
    private Cache<String, String> localCache;
    private Cache<String, String> remoteCache;
    private DistributedLock lock;
    private SimpleMultiLevelCache<String, String> multiLevelCache;

    // Redis相关
    private RedissonClient redissonClient;
    private RedisTemplate<String, Object> redisTemplate;
    private Serializer serializer;

    @BeforeEach
    public void setUp() {
        // 创建本地缓存
        CacheConfig localConfig = CacheConfig.builder()
                .localExpireSeconds(60)
                .localMaxSize(100)
                .build();
        localCache = new CaffeineLocalCache<>("testLocal", localConfig);

        // 创建Redisson客户端
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setDatabase(0)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(2)
                .setPassword("123456");

        try {
            redissonClient = Redisson.create(config);

            // 创建RedisTemplate
            RedissonConnectionFactory connectionFactory = new RedissonConnectionFactory(redissonClient);
            redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(connectionFactory);
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(RedisSerializer.json());
            redisTemplate.setHashKeySerializer(new StringRedisSerializer());
            redisTemplate.setHashValueSerializer(RedisSerializer.json());
            redisTemplate.afterPropertiesSet();

            // 序列化器
            serializer = new JsonSerializer();

            // 创建远程缓存
            CacheConfig remoteConfig = CacheConfig.builder()
                    .remoteExpireSeconds(300)
                    .build();

            remoteCache = new RedisRemoteCache<>(
                    "testRemote",
                    remoteConfig,
                    redisTemplate,
                    serializer,
                    null,
                    null);

            // 清空Redis测试键
            try {
                redisTemplate.delete(redisTemplate.keys("testRemote:*"));
                redisTemplate.delete(redisTemplate.keys("testMultiLevel:*"));
            } catch (Exception e) {
                System.err.println("清空Redis测试键失败: " + e.getMessage());

                // 使用模拟的远程缓存作为后备
                useRedisMock();
            }
        } catch (Exception e) {
            System.err.println("Redis连接失败，将使用模拟实现: " + e.getMessage());
            // 如果Redis连接失败，使用模拟
            useRedisMock();
        }

        // 创建分布式锁
        lock = new DefaultDistributedLock();

        // 创建多级缓存
        multiLevelCache = new SimpleMultiLevelCache<>(
                "testMultiLevel", localConfig, localCache, remoteCache, lock);
    }

    /**
     * 使用模拟的Redis缓存
     */
    private void useRedisMock() {
        // 创建远程缓存(Mock)
        remoteCache = Mockito.mock(Cache.class);
        Mockito.when(remoteCache.getName()).thenReturn("testRemote");
        Mockito.when(remoteCache.getConfig()).thenReturn(
                CacheConfig.builder().remoteExpireSeconds(300).build());
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
    }

    @Test
    public void testReadFromLocalCache() {
        // 准备：只在本地缓存中有数据
        localCache.put("localKey", "localValue");

        if (remoteCache instanceof RedisRemoteCache) {
            remoteCache.remove("localKey");
        } else {
            Mockito.when(remoteCache.get("localKey")).thenReturn(null);
        }

        // 从多级缓存读取
        String result = multiLevelCache.get("localKey");

        // 验证：应该从本地缓存读取到数据
        assertEquals("localValue", result);

        // 验证：没有尝试从远程缓存读取
        if (!(remoteCache instanceof RedisRemoteCache)) {
            Mockito.verify(remoteCache, Mockito.never()).get("localKey");
        }
    }

    @Test
    public void testReadFromRemoteCache() {
        // 准备：本地缓存miss，远程缓存hit
        if (remoteCache instanceof RedisRemoteCache) {
            remoteCache.put("remoteKey", "remoteValue");
        } else {
            Mockito.when(remoteCache.get("remoteKey")).thenReturn("remoteValue");
        }

        // 从多级缓存读取
        String result = multiLevelCache.get("remoteKey");

        // 验证：应该从远程缓存读取到数据
        assertEquals("remoteValue", result);

        // 验证：应该尝试从远程缓存读取
        if (!(remoteCache instanceof RedisRemoteCache)) {
            Mockito.verify(remoteCache, Mockito.times(1)).get("remoteKey");
        }

        // 验证：远程缓存的值被加载到本地缓存
        assertEquals("remoteValue", localCache.get("remoteKey"));
    }

    @Test
    public void testReadFromNoneCache() {
        if (remoteCache instanceof RedisRemoteCache) {
            remoteCache.remove("nonExistingKey");
        } else {
            Mockito.when(remoteCache.get("nonExistingKey")).thenReturn(null);
        }

        // 从多级缓存读取
        String result = multiLevelCache.get("nonExistingKey");

        // 验证：应该返回null
        assertNull(result);

        // 验证：尝试从远程缓存读取
        if (!(remoteCache instanceof RedisRemoteCache)) {
            Mockito.verify(remoteCache, Mockito.times(1)).get("nonExistingKey");
        }
    }

    @Test
    public void testWriteOperation() {
        // 写入多级缓存
        multiLevelCache.put("writeKey", "writeValue");

        // 验证：写入本地缓存
        assertEquals("writeValue", localCache.get("writeKey"));

        // 验证：写入远程缓存
        if (remoteCache instanceof RedisRemoteCache) {
            assertEquals("writeValue", remoteCache.get("writeKey"));
        } else {
            Mockito.verify(remoteCache, Mockito.times(1)).put("writeKey", "writeValue");
        }
    }

    @Test
    public void testWriteWithExpireTime() {
        // 写入多级缓存(带过期时间)
        multiLevelCache.put("expireKey", "expireValue", 120);

        // 验证：写入本地缓存
        assertEquals("expireValue", localCache.get("expireKey"));

        // 验证：写入远程缓存(带过期时间)
        if (remoteCache instanceof RedisRemoteCache) {
            assertEquals("expireValue", remoteCache.get("expireKey"));
        } else {
            Mockito.verify(remoteCache, Mockito.times(1))
                    .put("expireKey", "expireValue", 120);
        }
    }

    @Test
    public void testRemoveOperation() {
        // 准备：先写入数据
        multiLevelCache.put("removeKey", "removeValue");

        // 删除数据
        multiLevelCache.remove("removeKey");

        // 验证：从本地缓存删除
        assertNull(localCache.get("removeKey"));

        // 验证：从远程缓存删除
        if (remoteCache instanceof RedisRemoteCache) {
            assertNull(remoteCache.get("removeKey"));
        } else {
            Mockito.verify(remoteCache, Mockito.times(1)).remove("removeKey");
        }
    }

    @Test
    public void testClearOperation() {
        // 准备：写入多条数据
        multiLevelCache.put("key1", "value1");
        multiLevelCache.put("key2", "value2");

        // 清空缓存
        multiLevelCache.clear();

        // 验证：本地缓存被清空
        assertNull(localCache.get("key1"));
        assertNull(localCache.get("key2"));

        // 验证：远程缓存被清空
        if (remoteCache instanceof RedisRemoteCache) {
            assertNull(remoteCache.get("key1"));
            assertNull(remoteCache.get("key2"));
        } else {
            Mockito.verify(remoteCache, Mockito.times(1)).clear();
        }
    }

    @Test
    public void testContainsKey() {
        // 准备：在本地缓存中写入数据
        localCache.put("localOnlyKey", "localValue");

        // 准备：在远程缓存中有数据
        if (remoteCache instanceof RedisRemoteCache) {
            remoteCache.put("remoteOnlyKey", "remoteValue");
            // 确保本地缓存没有这个键
            localCache.remove("remoteOnlyKey");
        } else {
            Mockito.when(remoteCache.containsKey("remoteOnlyKey")).thenReturn(true);
            Mockito.when(remoteCache.containsKey("localOnlyKey")).thenReturn(false);
            Mockito.when(remoteCache.containsKey("nonExistingKey")).thenReturn(false);
        }

        // 验证：本地有的键
        assertEquals(true, multiLevelCache.containsKey("localOnlyKey"));

        // 验证：远程有的键
        assertEquals(true, multiLevelCache.containsKey("remoteOnlyKey"));

        // 验证：两者都没有的键
        assertEquals(false, multiLevelCache.containsKey("nonExistingKey"));
    }

    @Test
    public void testDataConsistency() {
        // 模拟场景：先写入多级缓存，然后远程缓存更新，本地缓存没有更新

        // 1. 写入初始数据
        multiLevelCache.put("consistencyKey", "initialValue");

        // 2. 模拟远程缓存被另一个实例更新
        if (remoteCache instanceof RedisRemoteCache) {
            remoteCache.put("consistencyKey", "updatedValue");
        } else {
            Mockito.when(remoteCache.get("consistencyKey")).thenReturn("updatedValue");
        }

        // 3. 手动清除本地缓存，模拟缓存过期
        localCache.remove("consistencyKey");

        // 4. 再次从多级缓存读取
        String result = multiLevelCache.get("consistencyKey");

        // 验证：应该获得远程缓存中的最新值
        assertEquals("updatedValue", result);

        // 验证：本地缓存应该被更新为最新值
        assertEquals("updatedValue", localCache.get("consistencyKey"));
    }
}