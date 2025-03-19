package com.easy.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.easy.cache.annotation.CacheInvalidate;
import com.easy.cache.annotation.Cached;
import com.easy.cache.annotation.EnableCaching;
import com.easy.cache.builder.CacheBuilder;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheManager;
import com.easy.cache.implementation.local.CaffeineLocalCache;
import com.easy.cache.implementation.remote.RedisRemoteCache;
import com.easy.cache.serialization.JsonSerializer;
import com.easy.cache.serialization.Serializer;

/**
 * 注解驱动缓存测试
 * 测试@Cached, @CacheUpdate, @CacheInvalidate注解的功能
 * 
 * 运行测试前请确保Redis服务可用
 * 如需跳过Redis相关测试，请使用: mvn test -DskipRedisTests=true
 */
@SpringBootTest
public class AnnotationDrivenCacheTest {

    /**
     * 是否跳过Redis测试的系统属性名
     */
    private static final String SKIP_REDIS_TESTS_PROPERTY = "skipRedisTests";

    @TestConfiguration
    @EnableCaching
    @EnableAspectJAutoProxy
    static class TestConfig {

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

        @Bean
        public RedisConnectionFactory redisConnectionFactory(RedissonClient redissonClient) {
            return new RedissonConnectionFactory(redissonClient);
        }

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

        @Bean
        public Serializer serializer() {
            return new JsonSerializer();
        }

        @Bean
        public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate, Serializer serializer) {
            // 创建本地缓存
            Cache<Object, Object> localCache = new CaffeineLocalCache<>("localCache",
                    CacheConfig.builder()
                            .localExpireSeconds(60)
                            .localMaxSize(1000)
                            .build());

            // 创建远程缓存
            Cache<Object, Object> remoteCache = new RedisRemoteCache<>(
                    "remoteCache",
                    CacheConfig.builder()
                            .remoteExpireSeconds(600)
                            .remoteMaxSize(10000)
                            .build(),
                    redisTemplate,
                    serializer,
                    null,
                    null);

            return CacheBuilder.builder()
                    .withLocalCache(localCache)
                    .withRemoteCache(remoteCache)
                    .withConfig(CacheConfig.builder()
                            .localExpireSeconds(60)
                            .remoteExpireSeconds(600)
                            .build())
                    .build();
        }

        @Bean
        public UserService userService() {
            return new UserService();
        }
    }

    // 测试服务类
    public static class UserService {

        private int methodCallCount = 0;

        @Cached(name = "userCache", key = "#userId", expire = 60)
        public User getUserById(String userId) {
            methodCallCount++;
            if ("invalid".equals(userId)) {
                return null;
            }
            return new User(userId, "User " + userId, 20 + methodCallCount);
        }

        @CacheInvalidate(name = "userCache", key = "#user.id")
        public void updateUser(User user) {
            // 模拟数据库更新
            System.out.println("更新用户: " + user);
        }

        @CacheInvalidate(name = "userCache", key = "#userId")
        public void deleteUser(String userId) {
            // 模拟数据库删除
            System.out.println("删除用户: " + userId);
        }

        public int getMethodCallCount() {
            return methodCallCount;
        }

        public void resetMethodCallCount() {
            methodCallCount = 0;
        }
    }

    // 测试实体类
    public static class User {
        private String id;
        private String name;
        private int age;

        public User() {
        }

        public User(String id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{id='" + id + "', name='" + name + "', age=" + age + '}';
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired(required = false)
    private RedissonClient redissonClient;

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
    @EnabledIfSystemProperty(named = SKIP_REDIS_TESTS_PROPERTY, matches = "false|FALSE|no|NO|", disabledReason = "Redis测试被系统属性skipRedisTests禁用")
    public void testCachedAnnotation() {
        userService.resetMethodCallCount();

        // 首次调用，应该查询方法体
        User user1 = userService.getUserById("1001");
        assertNotNull(user1);
        assertEquals("1001", user1.getId());
        assertEquals(1, userService.getMethodCallCount());

        // 再次调用，应该从缓存获取
        User cachedUser = userService.getUserById("1001");
        assertNotNull(cachedUser);
        assertEquals("1001", cachedUser.getId());
        // 方法调用次数应该还是1，因为第二次是从缓存获取的
        assertEquals(1, userService.getMethodCallCount());

        // 验证返回了相同的实例（通过age判断，因为每次调用方法体都会增加age）
        assertEquals(user1.getAge(), cachedUser.getAge());
    }

    @Test
    @EnabledIfSystemProperty(named = SKIP_REDIS_TESTS_PROPERTY, matches = "false|FALSE|no|NO|", disabledReason = "Redis测试被系统属性skipRedisTests禁用")
    public void testCacheInvalidateAnnotation() {
        userService.resetMethodCallCount();

        // 第一次调用
        User user = userService.getUserById("1002");
        assertNotNull(user);
        assertEquals(1, userService.getMethodCallCount());

        // 再次调用，应该从缓存获取
        user = userService.getUserById("1002");
        assertEquals(1, userService.getMethodCallCount());

        // 调用无效缓存方法
        userService.deleteUser("1002");

        // 再次调用，因为缓存已无效，应该重新查询
        user = userService.getUserById("1002");
        assertEquals(2, userService.getMethodCallCount());
    }

    @Test
    @EnabledIfSystemProperty(named = SKIP_REDIS_TESTS_PROPERTY, matches = "false|FALSE|no|NO|", disabledReason = "Redis测试被系统属性skipRedisTests禁用")
    public void testNullValueCaching() {
        userService.resetMethodCallCount();

        // 查询不存在的用户，返回null
        User user = userService.getUserById("invalid");
        assertNull(user);
        assertEquals(1, userService.getMethodCallCount());

        // 再次查询，应该从缓存获取null
        user = userService.getUserById("invalid");
        assertNull(user);
        assertEquals(1, userService.getMethodCallCount());
    }

    @Test
    @EnabledIfSystemProperty(named = SKIP_REDIS_TESTS_PROPERTY, matches = "false|FALSE|no|NO|", disabledReason = "Redis测试被系统属性skipRedisTests禁用")
    public void testCacheThroughManager() {
        userService.resetMethodCallCount();

        // 通过服务调用
        User user1 = userService.getUserById("1003");
        assertNotNull(user1);
        assertEquals(1, userService.getMethodCallCount());

        // 查看缓存管理器中是否存在
        assertNotNull(cacheManager);
        // 缓存键应该是 "1003"，由keyGenerator生成
    }
}