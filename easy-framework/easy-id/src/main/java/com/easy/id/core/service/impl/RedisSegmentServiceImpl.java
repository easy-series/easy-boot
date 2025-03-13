package com.easy.id.core.service.impl;

import com.easy.id.core.model.Segment;
import com.easy.id.core.service.SegmentService;
import com.easy.id.exception.IdGeneratorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于Redis的号段服务实现
 */
@Slf4j
public class RedisSegmentServiceImpl implements SegmentService {

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    /**
     * Redis Lua脚本，用于原子性地获取和更新当前值
     */
    private static final String LUA_SCRIPT = "local key = KEYS[1] " +
            "local step = tonumber(ARGV[1]) " +
            "local exists = redis.call('exists', key) " +
            "if exists == 0 then " +
            "  return -1 " + // 返回-1表示键不存在
            "end " +
            "local current = tonumber(redis.call('get', key)) " +
            "redis.call('set', key, current + step) " +
            "return current";

    /**
     * Redis Lua脚本，用于初始化键
     */
    private static final String INIT_SCRIPT = "local key = KEYS[1] " +
            "local exists = redis.call('exists', key) " +
            "if exists == 0 then " +
            "  redis.call('set', key, 0) " +
            "  return 1 " + // 返回1表示初始化成功
            "end " +
            "return 0"; // 返回0表示键已存在

    private final RedisScript<Long> incrScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
    private final RedisScript<Long> initScript = new DefaultRedisScript<>(INIT_SCRIPT, Long.class);

    public RedisSegmentServiceImpl(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Segment getNextSegment(String businessKey, int step) {
        try {
            String redisKey = buildRedisKey(businessKey);
            Long currentValue = redisTemplate.execute(incrScript, Collections.singletonList(redisKey),
                    String.valueOf(step));

            if (currentValue == null || currentValue < 0) {
                throw new IdGeneratorException("业务键不存在: " + businessKey);
            }

            // 创建号段对象
            Segment segment = new Segment();
            segment.setBusinessKey(businessKey);
            segment.setStep(step);
            segment.setCurrentValue(new AtomicLong(currentValue));
            segment.setMaxValue(currentValue + step);
            return segment;
        } catch (Exception e) {
            log.error("获取号段失败", e);
            throw new IdGeneratorException("获取号段失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void initBusinessKey(String businessKey, int step) {
        try {
            String redisKey = buildRedisKey(businessKey);
            Long result = redisTemplate.execute(initScript, Collections.singletonList(redisKey));

            if (result == null) {
                throw new IdGeneratorException("初始化业务键失败: " + businessKey);
            }

            if (result == 1L) {
                log.info("初始化业务键成功: {}", businessKey);
            } else {
                log.info("业务键已存在: {}", businessKey);
            }
        } catch (Exception e) {
            log.error("初始化业务键失败", e);
            throw new IdGeneratorException("初始化业务键失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建Redis键
     */
    private String buildRedisKey(String businessKey) {
        return keyPrefix + ":" + businessKey;
    }
}