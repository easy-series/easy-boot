package com.easy.cache.sync.redis;

import org.springframework.data.redis.core.RedisTemplate;

import com.easy.cache.serialization.Serializer;
import com.easy.cache.sync.CacheEvent;
import com.easy.cache.sync.CacheEventPublisher;

/**
 * Redis实现的缓存事件发布器
 * 使用Redis的PubSub机制来发布缓存事件
 */
public class RedisEventPublisher implements CacheEventPublisher {

    /**
     * Redis频道前缀
     */
    private static final String CHANNEL_PREFIX = "cache:event:";

    /**
     * Redis操作模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 序列化器
     */
    private final Serializer serializer;

    /**
     * 构造方法
     *
     * @param redisTemplate Redis操作模板
     * @param serializer    序列化器
     */
    public RedisEventPublisher(RedisTemplate<String, Object> redisTemplate, Serializer serializer) {
        this.redisTemplate = redisTemplate;
        this.serializer = serializer;
    }

    /**
     * 发布缓存事件
     * 
     * @param event 缓存事件
     */
    @Override
    public void publish(CacheEvent event) {
        try {
            String channel = buildChannelName(event.getCacheName());
            // 直接发送事件对象，RedisTemplate会使用其配置的序列化器处理
            redisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            // 记录错误但不中断操作
            e.printStackTrace();
        }
    }

    /**
     * 辅助方法：发布缓存事件
     * 
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param oldValue  旧值
     * @param newValue  新值
     */
    public void publish(String cacheName, Object key, Object oldValue, Object newValue) {
        try {
            // 根据新值和旧值判断事件类型
            CacheEvent.EventType eventType;
            Object value;

            if (oldValue == null && newValue != null) {
                // 旧值为空，新值不为空，表示更新
                eventType = CacheEvent.EventType.UPDATE;
                value = newValue;
            } else if (oldValue != null && newValue == null) {
                // 旧值不为空，新值为空，表示删除
                eventType = CacheEvent.EventType.DELETE;
                value = null;
            } else if (key == null) {
                // 键为空，表示清空
                eventType = CacheEvent.EventType.CLEAR;
                value = null;
            } else {
                // 默认为更新
                eventType = CacheEvent.EventType.UPDATE;
                value = newValue;
            }

            // 创建缓存事件并发布
            CacheEvent event = new CacheEvent(cacheName, key, value, eventType);
            publish(event);
        } catch (Exception e) {
            // 记录错误但不中断操作
            e.printStackTrace();
        }
    }

    /**
     * 构建Redis频道名称
     * 
     * @param cacheName 缓存名称
     * @return Redis频道名称
     */
    private String buildChannelName(String cacheName) {
        return CHANNEL_PREFIX + cacheName;
    }

}
