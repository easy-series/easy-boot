package com.easy.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.ActiveProfiles;

import com.easy.cache.api.Cache;
import com.easy.cache.config.CacheType;
import com.easy.cache.config.QuickConfig;
import com.easy.cache.support.sync.CacheEventPublisher;
import com.easy.cache.support.sync.DefaultLocalCacheSyncManager;
import com.easy.cache.support.sync.LocalCacheSyncManager;
import com.easy.cache.support.sync.redis.RedisCacheEventPublisher;
import com.easy.cache.template.CacheTemplate;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot环境下的缓存框架测试
 * 使用真实Redis环境
 */
@SpringBootTest
@ActiveProfiles("test")
// 导入测试缓存配置，排除标准自动配置
public class SpringBootCacheTest {

    /**
     * 测试用户类
     */
    @Data
    static class User {
        private Long id;
        private String name;
        private int age;

        public User(Long id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    @Autowired
    private CacheTemplate cacheTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 测试缓存实例
     */
    private Cache<String, User> localCache;
    private Cache<String, User> remoteCache;
    private Cache<String, User> multiCache;

    /**
     * 在每个测试前设置缓存
     */
    @BeforeEach
    void setup() {
        // 创建测试缓存
        localCache = cacheTemplate.createCache(
                QuickConfig.newBuilder("testLocalCache")
                        .cacheType(CacheType.LOCAL)
                        .expire(Duration.ofMinutes(5))
                        .localLimit(100)
                        .build());

        remoteCache = cacheTemplate.createCache(
                QuickConfig.newBuilder("testRemoteCache")
                        .cacheType(CacheType.REMOTE)
                        .expire(Duration.ofMinutes(5))
                        .build());

        multiCache = cacheTemplate.createCache(
                QuickConfig.newBuilder("testMultiCache")
                        .cacheType(CacheType.BOTH)
                        .expire(Duration.ofMinutes(5))
                        .syncLocal(true)
                        .build());

        // 清空测试缓存
        localCache.clear();
        remoteCache.clear();
        multiCache.clear();

        // 清空Redis同步通道
        stringRedisTemplate.delete("easy:cache:sync");
    }

    /**
     * 测试本地缓存操作
     */
    @Test
    void testLocalCache() {
        String key = "local:" + UUID.randomUUID();
        User user = new User(1L, "本地缓存测试", 25);

        // 测试缓存不存在
        assertNull(localCache.get(key));

        // 测试写入缓存
        localCache.put(key, user);
        User cachedUser = localCache.get(key);
        assertNotNull(cachedUser);
        assertEquals(user.getId(), cachedUser.getId());
        assertEquals(user.getName(), cachedUser.getName());
        assertEquals(user.getAge(), cachedUser.getAge());

        // 测试更新缓存
        user.setAge(26);
        localCache.put(key, user);
        cachedUser = localCache.get(key);
        assertEquals(26, cachedUser.getAge());

        // 测试删除缓存
        boolean removed = localCache.remove(key);
        assertTrue(removed);
        assertNull(localCache.get(key));
    }

    /**
     * 测试远程缓存操作
     */
    @Test
    void testRemoteCache() {
        String key = "remote:" + UUID.randomUUID();
        User user = new User(2L, "远程缓存测试", 30);

        // 测试缓存不存在
        assertNull(remoteCache.get(key));

        // 测试写入缓存
        remoteCache.put(key, user);
        User cachedUser = remoteCache.get(key);
        assertNotNull(cachedUser);
        assertEquals(user.getId(), cachedUser.getId());
        assertEquals(user.getName(), cachedUser.getName());
        assertEquals(user.getAge(), cachedUser.getAge());

        // 直接检查Redis中的值是否存在
        boolean exists = stringRedisTemplate.hasKey("testRemoteCache:" + key);
        assertTrue(exists);

        // 测试删除缓存
        boolean removed = remoteCache.remove(key);
        assertTrue(removed);
        assertNull(remoteCache.get(key));

        // 确保Redis中的值也被删除
        exists = stringRedisTemplate.hasKey("testRemoteCache:" + key);
        assertFalse(exists);
    }

    /**
     * 测试多级缓存操作
     */
    @Test
    void testMultiLevelCache() {
        String key = "multi:" + UUID.randomUUID();
        User user = new User(3L, "多级缓存测试", 35);

        // 测试缓存不存在
        assertNull(multiCache.get(key));

        // 测试写入缓存
        multiCache.put(key, user);

        // 应该同时写入本地和远程缓存
        User cachedUser = multiCache.get(key);
        assertNotNull(cachedUser);
        assertEquals(user.getId(), cachedUser.getId());
        assertEquals(user.getName(), cachedUser.getName());

        // 检查Redis中是否存在
        boolean exists = stringRedisTemplate.hasKey("testMultiCache:" + key);
        assertTrue(exists);

        // 测试过期时间
        // 使用较短的过期时间进行测试
        String shortExpireKey = "shortExpire:" + UUID.randomUUID();
        multiCache.put(shortExpireKey, user, Duration.ofSeconds(1));

        // 立即检查应该可以获取到
        assertNotNull(multiCache.get(shortExpireKey));

        // 等待过期
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 过期后应该获取不到
        assertNull(multiCache.get(shortExpireKey));
    }

    /**
     * 测试缓存计算
     */
    @Test
    void testComputeIfAbsent() {
        String key = "compute:" + UUID.randomUUID();

        // 使用computeIfAbsent获取不存在的值
        User user = multiCache.computeIfAbsent(key, k -> new User(4L, "计算缓存", 40));
        assertNotNull(user);
        assertEquals(4L, user.getId());
        assertEquals("计算缓存", user.getName());
        assertEquals(40, user.getAge());

        // 再次获取应该从缓存返回，而不是重新计算
        User cachedUser = multiCache.computeIfAbsent(key, k -> new User(999L, "错误数据", 999));
        assertEquals(4L, cachedUser.getId());
        assertEquals("计算缓存", cachedUser.getName());
        assertEquals(40, cachedUser.getAge());

        // 验证Redis中也有缓存
        boolean exists = stringRedisTemplate.hasKey("testMultiCache:" + key);
        assertTrue(exists);
    }

    /**
     * 测试缓存null值
     */
    @Test
    void testCacheNullValues() {
        // 创建一个可以缓存null值的缓存
        Cache<String, User> nullCache = cacheTemplate.createCache(
                QuickConfig.newBuilder("nullCache")
                        .cacheType(CacheType.BOTH)
                        .cacheNullValues(true)
                        .build());

        String key = "null:" + UUID.randomUUID();

        // 缓存一个null值
        User nullUser = nullCache.computeIfAbsent(key, k -> null);
        assertNull(nullUser);

        // 再次获取，应该仍然返回null，而不是计算新值
        nullUser = nullCache.computeIfAbsent(key, k -> new User(999L, "不应该出现", 999));
        assertNull(nullUser);

        // 验证Redis中是否有对应的key（虽然值为null）
        boolean exists = stringRedisTemplate.hasKey("nullCache:" + key);
        assertTrue(exists);
    }

    /**
     * 服务层缓存测试
     */
    @Nested
    class ServiceLayerTest {

        /**
         * 用户数据仓库接口（模拟数据库访问）
         */
        interface UserRepository {
            User findById(Long id);

            void save(User user);
        }

        /**
         * 用户服务类，使用缓存
         */
        @RequiredArgsConstructor
        @Slf4j
        static class UserService {
            private final CacheTemplate cacheTemplate;
            private final UserRepository userRepository;
            private final Cache<Long, User> userCache;
            private final String cacheKey = "user";

            public UserService(CacheTemplate cacheTemplate, UserRepository userRepository) {
                this.cacheTemplate = cacheTemplate;
                this.userRepository = userRepository;
                // 创建用户缓存，使用双层缓存策略
                this.userCache = cacheTemplate.createCache(
                        QuickConfig.newBuilder(cacheKey)
                                .cacheType(CacheType.BOTH) // 使用多级缓存
                                .expire(Duration.ofHours(1)) // 缓存1小时
                                .localLimit(1000) // 本地最多缓存1000个用户
                                .syncLocal(true) // 开启本地缓存同步
                                .penetrationProtect(true) // 开启缓存穿透保护
                                .build());
            }

            /**
             * 获取用户，优先从缓存获取
             */
            public User getUser(Long id) {
                // 使用缓存模板，如果缓存不存在则从数据库加载
                return userCache.computeIfAbsent(id, this::loadUserFromDb);
            }

            /**
             * 更新用户信息，同时更新缓存
             */
            public void updateUser(User user) {
                // 先更新数据库
                userRepository.save(user);
                // 再更新缓存
                userCache.put(user.getId(), user);
                // 或者也可以选择直接删除缓存
                // userCache.remove(user.getId());
            }

            /**
             * 从数据库加载用户
             */
            private User loadUserFromDb(Long id) {
                log.debug("从数据库加载用户: id={}", id);
                return userRepository.findById(id);
            }
        }

        /**
         * 测试服务层缓存使用
         */
        @Test
        void testServiceWithCache() {
            // 模拟数据库操作计数器
            AtomicInteger dbQueryCount = new AtomicInteger(0);

            // 模拟数据库存储的用户
            User dbUser = new User(10L, "张三", 30);

            // 创建模拟的UserRepository
            UserRepository mockRepo = mock(UserRepository.class);
            when(mockRepo.findById(10L)).thenAnswer(invocation -> {
                dbQueryCount.incrementAndGet(); // 记录查询次数
                return dbUser;
            });

            // 创建用户服务
            UserService userService = new UserService(cacheTemplate, mockRepo);

            // 第一次调用，应该查询数据库
            User user1 = userService.getUser(10L);
            assertEquals(dbUser.getName(), user1.getName());
            assertEquals(1, dbQueryCount.get()); // 应该调用一次数据库

            // 第二次调用，应该从缓存获取
            User user2 = userService.getUser(10L);
            assertEquals(dbUser.getName(), user2.getName());
            assertEquals(1, dbQueryCount.get()); // 数据库调用次数不变

            // 更新用户
            User updatedUser = new User(10L, "李四", 32);
            userService.updateUser(updatedUser);

            // 验证数据库保存被调用
            verify(mockRepo, times(1)).save(updatedUser);

            // 再次获取用户，应该返回更新后的值
            User user3 = userService.getUser(10L);
            assertEquals("李四", user3.getName());
            assertEquals(32, user3.getAge());

            // 确认仍然只查询数据库一次
            assertEquals(1, dbQueryCount.get());
        }
    }

    /**
     * Spring Boot测试配置
     */
    @SpringBootApplication
    static class TestConfig {
        // 自动使用配置文件中的Redis配置

        /**
         * 创建Redis模板，专门用于缓存
         */
        @Bean
        public RedisTemplate<String, byte[]> cacheRedisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, byte[]> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.afterPropertiesSet();
            return template;
        }

        /**
         * 创建StringRedisTemplate（如果需要）
         */
        @Bean
        public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
            return new StringRedisTemplate(connectionFactory);
        }

        /**
         * 创建缓存事件发布器
         */
        @Bean
        public CacheEventPublisher cacheEventPublisher(StringRedisTemplate redisTemplate,
                @Value("${easy.cache.sync-channel:easy:cache:sync}") String syncChannel) {
            return new RedisCacheEventPublisher(redisTemplate, syncChannel);
        }

        /**
         * 创建本地缓存同步管理器
         */
        @Bean
        public LocalCacheSyncManager localCacheSyncManager() {
            return new DefaultLocalCacheSyncManager();
        }
    }
}