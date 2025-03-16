package com.easy.lock.core.executor;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis锁执行器实现
 */
@RequiredArgsConstructor
public class RedisLockExecutor implements LockExecutor {

    private final StringRedisTemplate redisTemplate;

    // Redis分布式锁的Lua脚本
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";

    // 预编译脚本提高性能
    private static final RedisScript<Long> UNLOCK_REDIS_SCRIPT = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

    @Override
    public boolean acquire(String key, String value, long expire) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, expire, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public boolean release(String key, String value) {
        // 使用Lua脚本保证原子性操作
        Long result = redisTemplate.execute(
                UNLOCK_REDIS_SCRIPT,
                Collections.singletonList(key),
                value);
        return Long.valueOf(1).equals(result);
    }

    @Override
    public boolean isLocked(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}