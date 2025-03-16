package com.easy.cache.test;

import com.easy.cache.annotation.CacheInvalidate;
import com.easy.cache.annotation.CachePenetrationProtect;
import com.easy.cache.annotation.CacheRefresh;
import com.easy.cache.annotation.CacheUpdate;
import com.easy.cache.annotation.Cached;
import com.easy.cache.core.CacheType;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务，用于测试缓存功能
 */
@Service
public class UserService {
    
    /**
     * 模拟数据库存储
     */
    private final Map<Long, User> userDb = new ConcurrentHashMap<>();
    
    /**
     * 调用计数
     */
    private final AtomicLong callCount = new AtomicLong(0);
    
    /**
     * 初始化一些测试数据
     */
    public UserService() {
        userDb.put(1L, new User(1L, "张三", 25));
        userDb.put(2L, new User(2L, "李四", 30));
        userDb.put(3L, new User(3L, "王五", 28));
    }
    
    /**
     * 根据ID获取用户（基本缓存）
     * 
     * @param userId 用户ID
     * @return 用户对象
     */
    @Cached(expire = 3600, cacheType = CacheType.LOCAL)
    public User getUserById(Long userId) {
        callCount.incrementAndGet();
        return userDb.get(userId);
    }
    
    /**
     * 根据ID获取用户（带刷新和穿透保护）
     * 
     * @param userId 用户ID
     * @return 用户对象
     */
    @Cached(name = "userCache-", key = "#userId", expire = 3600, cacheType = CacheType.LOCAL, cacheNull = true)
    @CacheRefresh(refresh = 1800, stopRefreshAfterLastAccess = 3600)
    @CachePenetrationProtect
    public User getUserByIdWithProtect(Long userId) {
        callCount.incrementAndGet();
        return userDb.get(userId);
    }
    
    /**
     * 更新用户
     * 
     * @param user 用户对象
     * @return 更新后的用户
     */
    @CacheUpdate(name = "userCache-", key = "#user.id", value = "#user")
    public User updateUser(User user) {
        userDb.put(user.getId(), user);
        return user;
    }
    
    /**
     * 删除用户
     * 
     * @param userId 用户ID
     */
    @CacheInvalidate(name = "userCache-", key = "#userId")
    public void deleteUser(Long userId) {
        userDb.remove(userId);
    }
    
    /**
     * 获取调用次数
     * 
     * @return 调用次数
     */
    public long getCallCount() {
        return callCount.get();
    }
    
    /**
     * 重置调用次数
     */
    public void resetCallCount() {
        callCount.set(0);
    }
    
    /**
     * 用户实体类
     */
    public static class User {
        private Long id;
        private String name;
        private int age;
        
        public User() {
        }
        
        public User(Long id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
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
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
} 