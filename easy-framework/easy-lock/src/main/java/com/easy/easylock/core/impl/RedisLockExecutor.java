package com.easy.easylock.core.impl;

import com.easy.easylock.core.Lock;
import com.easy.easylock.core.LockExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的分布式锁实现
 */
public class RedisLockExecutor implements LockExecutor {

    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_LOCK_PREFIX = "easy:lock:";

    // 使用Lua脚本保证获取锁的原子性
    private static final String LOCK_SCRIPT = "if redis.call('exists', KEYS[1]) == 0 then " +
            "   redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]); " +
            "   return 1; " +
            "end; " +
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   redis.call('expire', KEYS[1], ARGV[2]); " +
            "   return 1; " +
            "end; " +
            "return 0;";

    // 使用Lua脚本保证释放锁的原子性
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   redis.call('del', KEYS[1]); " +
            "   return 1; " +
            "end; " +
            "return 0;";

    private final RedisScript<Long> lockScript = new DefaultRedisScript<>(LOCK_SCRIPT, Long.class);
    private final RedisScript<Long> unlockScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

    public RedisLockExecutor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(Lock lock) {
        // 如果锁的值为空，则生成一个唯一标识作为锁的值
        if (lock.getValue() == null || lock.getValue().isEmpty()) {
            lock.setValue(UUID.randomUUID().toString());
        }

        // 获取锁的完整key
        String lockKey = REDIS_LOCK_PREFIX + lock.getFullName();

        // 过期时间(毫秒)
        long expireTime = lock.getTimeUnit().toMillis(lock.getLeaseTime());

        // 获取锁的等待时间
        long waitTime = lock.getTimeUnit().toMillis(lock.getWaitTime());
        long startTime = System.currentTimeMillis();

        do {
            // 执行Lua脚本尝试获取锁
            Long result = redisTemplate.execute(
                    lockScript,
                    Collections.singletonList(lockKey),
                    lock.getValue(),
                    String.valueOf(expireTime));

            // 获取锁成功
            if (result != null && result == 1L) {
                return true;
            }

            // 等待一段时间后重试
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.currentTimeMillis() - startTime < waitTime);

        // 获取锁失败
        return false;
    }

    @Override
    public boolean releaseLock(Lock lock) {
        // 获取锁的完整key
        String lockKey = REDIS_LOCK_PREFIX + lock.getFullName();

        // 执行Lua脚本释放锁
        Long result = redisTemplate.execute(
                unlockScript,
                Collections.singletonList(lockKey),
                lock.getValue());

        return result != null && result == 1L;
    }
}