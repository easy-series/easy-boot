package com.easy.easylock.example;

import com.easy.easylock.annotation.EasyLock;
import com.easy.easylock.utils.LockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁使用示例
 */
@Slf4j
@Service
public class LockExample {

    /**
     * 使用注解方式加锁
     */
    @EasyLock(name = "user:create", key = "#userId", waitTime = 5000, leaseTime = 30000)
    public void createUser(Long userId, String username) {
        log.info("创建用户，用户ID: {}, 用户名: {}", userId, username);
        // 模拟业务处理
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 使用编程方式加锁 - 带返回值
     */
    public String updateUser(Long userId, String newUsername) {
        return LockUtil.executeWithLock("user:update", userId.toString(), () -> {
            log.info("更新用户，用户ID: {}, 新用户名: {}", userId, newUsername);
            // 模拟业务处理
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "更新成功: " + newUsername;
        });
    }

    /**
     * 使用编程方式加锁 - 无返回值
     */
    public void deleteUser(Long userId) {
        LockUtil.executeWithLock("user:delete", userId.toString(), () -> {
            log.info("删除用户，用户ID: {}", userId);
            // 模拟业务处理
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}