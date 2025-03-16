package com.easy.cache.support.sync.redis;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.alibaba.fastjson2.JSON;
import com.easy.cache.support.sync.CacheEvent;
import com.easy.cache.support.sync.CacheEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于Redis的缓存事件发布器实现
 */
@Slf4j
@RequiredArgsConstructor
public class RedisCacheEventPublisher implements CacheEventPublisher {

    /**
     * Redis模板
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 广播通道
     */
    private final String broadcastChannel;

    @Override
    public void publish(CacheEvent event) {
        try {
            String message = JSON.toJSONString(event);
            redisTemplate.convertAndSend(broadcastChannel, message);
            log.debug("发布缓存事件: {}", event);
        } catch (Exception e) {
            log.error("发布缓存事件失败: {}", event, e);
        }
    }
}