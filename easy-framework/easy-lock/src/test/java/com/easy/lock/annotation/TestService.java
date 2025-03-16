package com.easy.lock.annotation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 测试服务类，用于测试锁注解
 */
public class TestService {

    private final Map<Long, String> userMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> stockMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> orderMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> resourceMap = new ConcurrentHashMap<>();

    /**
     * 基本的分布式锁注解使用
     */
    @EasyLock(key = "#userId", prefix = "user:update")
    public void updateUser(Long userId, String name) {
        // 模拟业务处理
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        userMap.put(userId, name);
    }

    /**
     * 获取用户名（无锁）
     */
    public String getUserName(Long userId) {
        return userMap.get(userId);
    }

    /**
     * 高并发场景下的分布式锁注解
     */
    @EasyLock(key = "#productId", prefix = "product:stock")
    public boolean updateStock(String productId, int quantity) {
        // 检查是否已经更新过库存，模拟一些业务逻辑
        if (stockMap.containsKey(productId)) {
            return false; // 库存已被其他线程更新
        }

        // 模拟业务处理
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 更新库存
        stockMap.put(productId, quantity);
        return true;
    }

    /**
     * 使用SpEL表达式的锁注解
     */
    @EasyLock(key = "#order.orderNo", prefix = "order:process")
    public boolean processOrder(EasyLockAnnotationTest.TestOrder order) {
        String orderNo = order.getOrderNo();

        // 模拟业务处理
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 处理订单
        orderMap.put(orderNo, true);
        return true;
    }

    /**
     * 带重试策略的锁注解
     */
    @EasyLock(key = "#resourceId", prefix = "resource", expire = 5000, retryCount = 3, retryInterval = 200)
    public boolean accessResourceWithRetry(String resourceId) {
        // 模拟业务处理
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 标记资源已访问
        resourceMap.put(resourceId, true);
        return true;
    }

    /**
     * 自定义失败策略
     */
    @EasyLock(key = "#resourceId", prefix = "custom", failStrategy = EasyLock.FailStrategy.IGNORE)
    public boolean customFailStrategy(String resourceId, EasyLock.FailStrategy strategy) {
        // 模拟业务处理
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        resourceMap.put(resourceId, true);
        return true;
    }
}