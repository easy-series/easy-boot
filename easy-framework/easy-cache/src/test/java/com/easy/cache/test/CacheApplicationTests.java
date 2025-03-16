package com.easy.cache.test;

import com.easy.cache.annotation.EnableMethodCache;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheTemplate;
import com.easy.cache.core.CacheType;
import com.easy.cache.test.UserService.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 缓存功能测试
 */
@SpringBootTest(classes = CacheApplicationTests.TestApplication.class)
public class CacheApplicationTests {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CacheTemplate cacheTemplate;
    
    /**
     * 测试注解方式缓存
     */
    @Test
    public void testAnnotationCache() {
        userService.resetCallCount();
        
        // 第一次调用，会触发方法执行
        User user1 = userService.getUserById(1L);
        assertNotNull(user1);
        assertEquals(1, userService.getCallCount());
        
        // 第二次调用，应该走缓存
        User user2 = userService.getUserById(1L);
        assertNotNull(user2);
        assertEquals(1, userService.getCallCount());  // 计数不变，说明走了缓存
        
        // 测试缓存更新
        user1.setName("张三修改");
        userService.updateUser(user1);
        
        // 再次获取，应该是更新后的数据
        User user3 = userService.getUserById(1L);
        assertEquals("张三修改", user3.getName());
        
        // 测试缓存失效
        userService.deleteUser(1L);
        
        // 再次获取，应该走方法
        User user4 = userService.getUserById(1L);
        assertNull(user4);
        assertEquals(2, userService.getCallCount());  // 计数+1，说明方法被调用
    }
    
    /**
     * 测试API方式缓存
     */
    @Test
    public void testApiCache() {
        // 创建缓存
        Cache<Long, User> userCache = cacheTemplate.createCache(
                "userApiCache", Duration.ofHours(1), CacheType.LOCAL);
        
        // 测试基本操作
        userCache.put(1L, new User(1L, "张三API", 25));
        
        User user = userCache.get(1L);
        assertNotNull(user);
        assertEquals("张三API", user.getName());
        
        // 测试computeIfAbsent
        User user2 = userCache.computeIfAbsent(2L, new Function<Long, User>() {
            @Override
            public User apply(Long id) {
                return new User(id, "李四API", 30);
            }
        });
        
        assertNotNull(user2);
        assertEquals("李四API", user2.getName());
        
        // 再次获取，验证已缓存
        User user3 = userCache.get(2L);
        assertNotNull(user3);
        assertEquals("李四API", user3.getName());
        
        // 测试删除
        boolean removed = userCache.remove(1L);
        assertEquals(true, removed);
        
        User userAfterRemove = userCache.get(1L);
        assertNull(userAfterRemove);
    }
    
    /**
     * 测试应用配置
     */
    @EnableMethodCache(basePackages = "com.easy.cache.test")
    @SpringBootApplication
    public static class TestApplication {
        
        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }
} 