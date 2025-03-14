package com.easy.easylock.core.impl;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import com.easy.easylock.core.LockExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于Redis实现的分布式锁
 */
@Slf4j
public class RedisLockExecutor implements LockExecutor {

    private final StringRedisTemplate redisTemplate;

    /**
     * 锁前缀
     */
    private final String keyPrefix;

    /**
     * 获取锁的Lua脚本
     * 保证原子性操作：判断键不存在则设置值和过期时间
     */
    private static final String LOCK_SCRIPT = "if redis.call('exists', KEYS[1]) == 0 then " +
            "redis.call('set', KEYS[1], ARGV[1]); " +
            "redis.call('pexpire', KEYS[1], ARGV[2]); " +
            "return 1; " +
            "end; " +
            "return 0;";

    /**
     * 释放锁的Lua脚本
     * 保证原子性操作：判断键存在且值匹配时才删除键
     */
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]); " +
            "end; " +
            "return 0;";

    private final RedisScript<Long> lockScript = new DefaultRedisScript<>(LOCK_SCRIPT, Long.class);
    private final RedisScript<Long> unlockScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

    /**
     * 构造函数
     * 
     * @param redisTemplate Redis模板
     * @param keyPrefix     锁前缀
     */
    public RedisLockExecutor(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public boolean tryLock(String lockKey, String lockValue, long waitTime, long leaseTime, TimeUnit timeUnit) {
        // 完整的锁键名
        final String fullLockKey = keyPrefix + lockKey;
        // 锁过期时间（毫秒）
        final long expireMillis = timeUnit.toMillis(leaseTime);
        // 获取锁截止时间
        final long deadline = System.currentTimeMillis() + timeUnit.toMillis(waitTime);

        boolean locked = false;
        // 在等待时间内尝试获取锁
        while (!locked && System.currentTimeMillis() < deadline) {
            try {
                Long result = redisTemplate.execute(
                        lockScript,
                        Collections.singletonList(fullLockKey),
                        lockValue, String.valueOf(expireMillis));
                locked = result != null && result == 1L;

                if (locked) {
                    log.debug("获取锁成功 - key: {}, value: {}", fullLockKey, lockValue);
                    break;
                }

                // 短暂等待后重试
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("获取锁异常 - key: {}, value: {}", fullLockKey, lockValue, e);
                break;
            }
        }

        return locked;
    }

    @Override
    public boolean releaseLock(String lockKey, String lockValue) {
        // 完整的锁键名
        final String fullLockKey = keyPrefix + lockKey;

        try {
            Long result = redisTemplate.execute(
                    unlockScript,
                    Collections.singletonList(fullLockKey),
                    lockValue);
            boolean released = result != null && result == 1L;

            if (released) {
                log.debug("释放锁成功 - key: {}, value: {}", fullLockKey, lockValue);
            } else {
                log.warn("释放锁失败 - key: {}, value: {}", fullLockKey, lockValue);
            }

            return released;
        } catch (Exception e) {
            log.error("释放锁异常 - key: {}, value: {}", fullLockKey, lockValue, e);
            return false;
        }
    }
}