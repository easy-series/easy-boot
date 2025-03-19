package com.easy.cache.implementation.remote;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.monitor.CacheStats;
import com.easy.cache.serialization.Serializer;
import com.easy.cache.sync.CacheEvent;
import com.easy.cache.sync.CacheEventPublisher;
import com.easy.cache.sync.CacheEventSubscriber;

/**
 * Redis远程缓存实现
 * 基于Spring Data Redis实现的远程缓存
 */
public class RedisRemoteCache<K, V> implements Cache<K, V> {

    private final String name;
    private final CacheConfig config;
    private final CacheStats stats;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Serializer serializer;
    private CacheEventPublisher eventPublisher;
    private CacheEventSubscriber eventSubscriber;

    /**
     * 构造方法
     *
     * @param name            缓存名称
     * @param config          缓存配置
     * @param redisTemplate   Redis操作模板
     * @param serializer      序列化器
     * @param eventPublisher  事件发布器
     * @param eventSubscriber 事件订阅器
     */
    public RedisRemoteCache(String name, CacheConfig config,
            RedisTemplate<String, Object> redisTemplate,
            Serializer serializer,
            CacheEventPublisher eventPublisher,
            CacheEventSubscriber eventSubscriber) {
        this.name = name;
        this.config = config;
        this.stats = new CacheStats();
        this.redisTemplate = redisTemplate;
        this.serializer = serializer;
        this.eventPublisher = eventPublisher;
        this.eventSubscriber = eventSubscriber;
    }

    /**
     * 设置事件发布器
     *
     * @param eventPublisher 事件发布器
     */
    public void setEventPublisher(CacheEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 设置事件订阅器
     *
     * @param eventSubscriber 事件订阅器
     */
    public void setEventSubscriber(CacheEventSubscriber eventSubscriber) {
        this.eventSubscriber = eventSubscriber;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public V get(K key) {
        String redisKey = buildKey(key);
        try {
            // 直接获取值，无需类型转换
            Object value = redisTemplate.opsForValue().get(redisKey);

            if (value != null) {
                stats.recordHit();

                // 处理不同类型的值
                if (value instanceof byte[]) {
                    // 如果是字节数组，使用序列化器反序列化
                    return serializer.deserialize((byte[]) value, (Class<V>) Object.class);
                } else {
                    // 直接返回或进行必要的转换
                    return (V) value;
                }
            } else {
                stats.recordMiss();
                return null;
            }
        } catch (Exception e) {
            stats.recordMiss();
            // 记录错误日志
            System.err.println("从Redis获取缓存失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, config.getRemoteExpireSeconds());
    }

    @Override
    public void put(K key, V value, long expireTime) {
        String redisKey = buildKey(key);
        try {
            // 直接存储值，不进行序列化
            if (expireTime > 0) {
                redisTemplate.opsForValue().set(redisKey, value, expireTime, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(redisKey, value);
            }

            // 发布缓存更新事件
            if (eventPublisher != null) {
                CacheEvent event = new CacheEvent(name, key, value, CacheEvent.EventType.UPDATE);
                eventPublisher.publish(event);
            }
        } catch (Exception e) {
            // 记录错误日志
            System.err.println("向Redis写入缓存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void remove(K key) {
        String redisKey = buildKey(key);
        try {
            redisTemplate.delete(redisKey);

            // 发布缓存删除事件
            if (eventPublisher != null) {
                CacheEvent event = new CacheEvent(name, key, null, CacheEvent.EventType.DELETE);
                eventPublisher.publish(event);
            }
        } catch (Exception e) {
            // 记录错误日志
            System.err.println("从Redis删除缓存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void clear() {
        try {
            String pattern = name + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));

            // 发布缓存清除事件
            if (eventPublisher != null) {
                CacheEvent event = new CacheEvent(name, null, null, CacheEvent.EventType.CLEAR);
                eventPublisher.publish(event);
            }
        } catch (Exception e) {
            // 记录错误日志
            System.err.println("清空Redis缓存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public CacheStats stats() {
        return stats;
    }

    @Override
    public CacheConfig getConfig() {
        return config;
    }

    @Override
    public boolean containsKey(K key) {
        String redisKey = buildKey(key);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            // 记录错误日志
            System.err.println("检查Redis键是否存在失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 构建Redis键
     *
     * @param key 缓存键
     * @return Redis键
     */
    private String buildKey(K key) {
        return name + ":" + key;
    }
}