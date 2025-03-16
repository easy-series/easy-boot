package com.easy.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.easy.cache.api.Cache;
import com.easy.cache.config.CacheConfig;
import com.easy.cache.config.CacheType;
import com.easy.cache.config.QuickConfig;
import com.easy.cache.core.embedded.CaffeineCache;
import com.easy.cache.support.convertor.FastJsonKeyConvertor;
import com.easy.cache.support.convertor.KeyConvertor;
import com.easy.cache.template.CacheTemplate;

import lombok.Data;

/**
 * 缓存框架测试类
 */
public class CacheTest {

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

    /**
     * 本地缓存单元测试
     */
    public static class LocalCacheUnitTest {
        private CaffeineCache<String, User> cache;
        private KeyConvertor keyConvertor;

        @BeforeEach
        void setup() {
            keyConvertor = new FastJsonKeyConvertor();

            // 创建本地缓存实例
            cache = new CaffeineCache<>(
                    "userCache",
                    keyConvertor,
                    Duration.ofMinutes(10),
                    null,
                    null,
                    100,
                    1000,
                    true,
                    null,
                    true,
                    true);
        }

        @Test
        void testGetPutAndRemove() {
            User user = new User(1L, "张三", 20);
            String key = "user:1";

            // 测试写入缓存
            cache.put(key, user);

            // 测试读取缓存
            User cachedUser = cache.get(key);
            assertEquals(user.getId(), cachedUser.getId());
            assertEquals(user.getName(), cachedUser.getName());
            assertEquals(user.getAge(), cachedUser.getAge());

            // 测试删除缓存
            boolean removed = cache.remove(key);
            assertTrue(removed);

            // 测试读取已删除的缓存
            cachedUser = cache.get(key);
            assertNull(cachedUser);
        }

        @Test
        void testComputeIfAbsent() {
            String key = "user:2";
            AtomicInteger counter = new AtomicInteger(0);

            // 测试计算并缓存
            User user = cache.computeIfAbsent(key, k -> {
                counter.incrementAndGet();
                return new User(2L, "李四", 30);
            });

            assertEquals(2L, user.getId());
            assertEquals("李四", user.getName());
            assertEquals(30, user.getAge());
            assertEquals(1, counter.get());

            // 再次获取，应该从缓存返回而不是重新计算
            user = cache.computeIfAbsent(key, k -> {
                counter.incrementAndGet();
                return new User(2L, "李四修改版", 35);
            });

            assertEquals(2L, user.getId());
            assertEquals("李四", user.getName());
            assertEquals(30, user.getAge());
            assertEquals(1, counter.get()); // 计数器不应该增加
        }

        @Test
        void testCacheNullValues() {
            String key = "user:null";
            AtomicInteger counter = new AtomicInteger(0);

            // 测试缓存空值
            User user = cache.computeIfAbsent(key, k -> {
                counter.incrementAndGet();
                return null;
            });

            assertNull(user);
            assertEquals(1, counter.get());

            // 再次获取，应该从缓存返回而不是重新计算
            user = cache.computeIfAbsent(key, k -> {
                counter.incrementAndGet();
                return new User(999L, "不应该出现", 999);
            });

            assertNull(user);
            assertEquals(1, counter.get()); // 计数器不应该增加
        }

        @Test
        void testCachePenetrationProtect() throws InterruptedException {
            String key = "user:slow";
            AtomicInteger counter = new AtomicInteger(0);

            // 模拟多线程同时请求缓存穿透保护
            Runnable task = () -> {
                User user = cache.computeIfAbsent(key, k -> {
                    counter.incrementAndGet();
                    try {
                        // 模拟慢操作
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return new User(3L, "慢查询", 40);
                });

                assertEquals(3L, user.getId());
                assertEquals("慢查询", user.getName());
            };

            // 创建并启动5个线程
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(task);
                threads[i].start();
            }

            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join();
            }

            // 由于缓存穿透保护，应该只计算一次
            assertEquals(1, counter.get());
        }
    }

    /**
     * 缓存模板集成测试
     */
    @SpringBootTest
    @ActiveProfiles("test")
    public static class CacheTemplateIntegrationTest {

        @Autowired
        private CacheTemplate cacheTemplate;

        @Autowired
        private StringRedisTemplate stringRedisTemplate;

        private Cache<String, User> localCache;
        private Cache<String, User> remoteCache;
        private Cache<String, User> multiCache;

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

        @Test
        void testLocalCacheOperations() {
            String key = "local:" + UUID.randomUUID().toString();
            User user = new User(1L, "本地缓存", 25);

            // 测试写入和读取
            localCache.put(key, user);
            User cachedUser = localCache.get(key);

            assertEquals(user.getId(), cachedUser.getId());
            assertEquals(user.getName(), cachedUser.getName());

            // 测试更新
            user.setAge(26);
            localCache.put(key, user);
            cachedUser = localCache.get(key);

            assertEquals(26, cachedUser.getAge());

            // 测试删除
            localCache.remove(key);
            assertNull(localCache.get(key));
        }

        @Test
        void testRemoteCacheOperations() {
            String key = "remote:" + UUID.randomUUID().toString();
            User user = new User(2L, "远程缓存", 30);

            // 测试写入和读取
            remoteCache.put(key, user);
            User cachedUser = remoteCache.get(key);

            assertEquals(user.getId(), cachedUser.getId());
            assertEquals(user.getName(), cachedUser.getName());

            // 直接检查Redis中的值是否存在
            boolean exists = stringRedisTemplate.hasKey("testRemoteCache:" + key);
            assertTrue(exists);

            // 测试删除
            remoteCache.remove(key);
            assertNull(remoteCache.get(key));

            // 确保Redis中的值已删除
            exists = stringRedisTemplate.hasKey("testRemoteCache:" + key);
            assertFalse(exists);
        }

        @Test
        void testMultiLevelCacheOperations() {
            String key = "multi:" + UUID.randomUUID().toString();
            User user = new User(3L, "多级缓存", 35);

            // 测试写入和读取
            multiCache.put(key, user);
            User cachedUser = multiCache.get(key);

            assertEquals(user.getId(), cachedUser.getId());
            assertEquals(user.getName(), cachedUser.getName());

            // 直接检查Redis中的值是否存在
            boolean exists = stringRedisTemplate.hasKey("testMultiCache:" + key);
            assertTrue(exists);

            // 测试更新
            user.setAge(36);
            multiCache.put(key, user);
            cachedUser = multiCache.get(key);

            assertEquals(36, cachedUser.getAge());

            // 测试删除
            multiCache.remove(key);
            assertNull(multiCache.get(key));

            // 确保Redis中的值已删除
            exists = stringRedisTemplate.hasKey("testMultiCache:" + key);
            assertFalse(exists);
        }

        @Test
        void testCacheTemplate() {
            String key = "template:" + UUID.randomUUID().toString();

            // 使用模板执行缓存计算
            User user = cacheTemplate.computeIfAbsent("testTemplate", key, k -> new User(4L, "模板缓存", 40));

            assertEquals(4L, user.getId());
            assertEquals("模板缓存", user.getName());
            assertEquals(40, user.getAge());

            // 再次获取，应该从缓存返回
            User cachedUser = cacheTemplate.computeIfAbsent("testTemplate", key, k -> new User(999L, "错误数据", 999));

            assertEquals(4L, cachedUser.getId());
            assertEquals("模板缓存", cachedUser.getName());
            assertEquals(40, cachedUser.getAge());
        }

        @Test
        void testLoadFunction() {
            AtomicInteger counter = new AtomicInteger(0);
            String key = "load:" + UUID.randomUUID().toString();

            // 创建带有加载函数的缓存
            Cache<String, User> loadingCache = cacheTemplate.createCache(
                    CacheConfig.<String, User>builder()
                            .name("loadingCache")
                            .cacheType(CacheType.LOCAL)
                            .expireAfterWrite(Duration.ofMinutes(5))
                            .loader(k -> {
                                counter.incrementAndGet();
                                return new User(5L, "加载函数", 45);
                            })
                            .build());

            // 首次获取，应该调用加载函数
            User user = loadingCache.get(key);

            assertEquals(5L, user.getId());
            assertEquals("加载函数", user.getName());
            assertEquals(45, user.getAge());
            assertEquals(1, counter.get());

            // 再次获取，应该从缓存返回
            user = loadingCache.get(key);

            assertEquals(5L, user.getId());
            assertEquals(1, counter.get()); // 计数器不应该增加
        }
    }

    /**
     * 模拟服务层测试
     */
    public static class ServiceLayerTest {

        @Test
        void testServiceWithCache() {
            // 模拟用户仓库
            UserRepository mockRepository = mock(UserRepository.class);
            when(mockRepository.findById(1L)).thenReturn(new User(1L, "测试用户", 30));

            // 创建缓存模板
            CacheTemplate cacheTemplate = mock(CacheTemplate.class);

            // 创建用户服务
            UserService userService = new UserService(cacheTemplate, mockRepository);

            // 调用获取用户方法
            User user = userService.getUser(1L);

            // 验证结果
            assertEquals(1L, user.getId());
            assertEquals("测试用户", user.getName());
            assertEquals(30, user.getAge());

            // 验证repository只被调用一次
            verify(mockRepository, times(1)).findById(1L);

            // 再次调用，应该从缓存获取
            user = userService.getUser(1L);

            // 验证repository不再被调用
            verify(mockRepository, times(1)).findById(1L);
        }

        // 模拟用户仓库接口
        interface UserRepository {
            User findById(Long id);

            void save(User user);
        }

        // 模拟用户服务类
        static class UserService {
            private final CacheTemplate cacheTemplate;
            private final UserRepository userRepository;
            private final Cache<Long, User> userCache;

            public UserService(CacheTemplate cacheTemplate, UserRepository userRepository) {
                this.cacheTemplate = cacheTemplate;
                this.userRepository = userRepository;

                // 在实际应用中，这里会创建实际的缓存
                // 但在测试中，我们使用模拟的方式
                when(cacheTemplate.computeIfAbsent(
                        org.mockito.ArgumentMatchers.eq("userCache"),
                        org.mockito.ArgumentMatchers.any(Long.class),
                        org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
                            Long id = invocation.getArgument(1);
                            return userRepository.findById(id);
                        });

                this.userCache = null; // 在测试中不使用
            }

            public User getUser(Long id) {
                return cacheTemplate.computeIfAbsent("userCache", id,
                        userId -> userRepository.findById(userId));
            }

            public void updateUser(User user) {
                userRepository.save(user);
                cacheTemplate.put("userCache", user.getId(), user);
            }
        }
    }
}