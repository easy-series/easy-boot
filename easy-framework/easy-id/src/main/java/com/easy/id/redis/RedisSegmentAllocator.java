package com.easy.id.redis;

import com.easy.id.exception.IdGeneratorException;
import com.easy.id.segment.dao.SegmentAllocator;
import com.easy.id.segment.dao.SegmentRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/**
 * 基于Redis的号段分配器实现
 *
 * 使用Redis的原子操作和Lua脚本实现号段分配，相比数据库方式具有更高的性能
 *
 * @author 芋道源码
 */
@Slf4j
public class RedisSegmentAllocator implements SegmentAllocator {

    /**
     * Redis操作模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 键前缀
     */
    private final String keyPrefix;

    /**
     * Lua脚本，用于原子性地获取和更新号段
     */
    private static final DefaultRedisScript<Long> NEXT_RANGE_SCRIPT = new DefaultRedisScript<>();

    /**
     * 初始化Lua脚本
     */
    static {
        StringBuilder builder = new StringBuilder();
        builder.append("local key = KEYS[1];\n");
        builder.append("local step = tonumber(ARGV[1]);\n");
        builder.append("local maxIdKey = key .. ':maxId';\n");
        builder.append("local stepKey = key .. ':step';\n");
        builder.append("local currentMaxId = tonumber(redis.call('get', maxIdKey)) or 0;\n");
        builder.append("local currentStep = tonumber(redis.call('get', stepKey)) or step;\n");
        builder.append("if step <= 0 then step = currentStep; end;\n");
        builder.append("local nextMaxId = currentMaxId + step;\n");
        builder.append("redis.call('set', maxIdKey, nextMaxId);\n");
        builder.append("if currentStep ~= step and step > 0 then redis.call('set', stepKey, step); end;\n");
        builder.append("return currentMaxId;");

        NEXT_RANGE_SCRIPT.setScriptText(builder.toString());
        NEXT_RANGE_SCRIPT.setResultType(Long.class);
    }

    /**
     * 构造函数
     * 
     * @param redisTemplate Redis操作模板
     */
    public RedisSegmentAllocator(RedisTemplate<String, Object> redisTemplate) {
        this(redisTemplate, "easy:id:segment:");
    }

    /**
     * 构造函数
     * 
     * @param redisTemplate Redis操作模板
     * @param keyPrefix     键前缀
     */
    public RedisSegmentAllocator(RedisTemplate<String, Object> redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public SegmentRange nextRange(String bizKey, int step) {
        String key = keyPrefix + bizKey;
        try {
            // 执行Lua脚本，原子性地获取和更新号段
            Long currentMaxId = redisTemplate.execute(
                    NEXT_RANGE_SCRIPT,
                    Collections.singletonList(key),
                    step);

            if (currentMaxId == null) {
                throw new IdGeneratorException("Redis执行号段分配脚本返回空值");
            }

            // 计算新的号段范围
            long minId = currentMaxId + 1;
            long maxId = currentMaxId + step;
            return new SegmentRange(minId, maxId, step);
        } catch (Exception e) {
            log.error("从Redis获取号段失败，bizKey: {}", bizKey, e);
            if (e instanceof IdGeneratorException) {
                throw e;
            }
            throw new IdGeneratorException("从Redis获取号段失败: " + e.getMessage(), e);
        }
    }
}