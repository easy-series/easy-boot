package com.easy.cache.event.redis;

import com.easy.cache.event.CacheEvent;
import com.easy.cache.event.CacheEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serialization.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serialization.RedisSerializer;

/**
 * 基于Redis的事件发布器实现
 */
public class RedisPublisher implements CacheEventPublisher {

    /**
     * Redis操作模板
     */
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Redis通道名称
     */
    private final String channelName;
    
    /**
     * 序列化器
     */
    private final RedisSerializer<Object> serializer;
    
    /**
     * 构造函数
     *
     * @param redisTemplate Redis操作模板
     * @param channelName Redis通道名称
     */
    public RedisPublisher(RedisTemplate<String, Object> redisTemplate, String channelName) {
        this.redisTemplate = redisTemplate;
        this.channelName = channelName;
        this.serializer = new JdkSerializationRedisSerializer();
    }
    
    /**
     * 默认构造函数，使用默认通道名称
     *
     * @param redisTemplate Redis操作模板
     */
    public RedisPublisher(RedisTemplate<String, Object> redisTemplate) {
        this(redisTemplate, "easy:cache:sync:channel");
    }
    
    @Override
    public void publish(CacheEvent event) {
        try {
            redisTemplate.convertAndSend(channelName, serializer.serialize(event));
        } catch (Exception e) {
            // 发布失败，记录日志
            System.err.println("Failed to publish cache event: " + e.getMessage());
        }
    }
} 