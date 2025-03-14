package com.easy.cache.core;

import com.easy.cache.serializer.Serializer;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存实现
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class RedisCache<K, V> extends AbstractCache<K, V> {

    /**
     * Redis操作模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 序列化器
     */
    private final Serializer serializer;

    /**
     * 默认过期时间
     */
    private final long defaultExpire;

    /**
     * 默认时间单位
     */
    private final TimeUnit defaultTimeUnit;

    /**
     * 构造函数
     *
     * @param name 缓存名称
     * @param redisTemplate Redis操作模板
     * @param serializer 序列化器
     * @param defaultExpire 默认过期时间
     * @param defaultTimeUnit 默认时间单位
     */
    public RedisCache(String name, RedisTemplate<String, Object> redisTemplate, 
                     Serializer serializer, long defaultExpire, TimeUnit defaultTimeUnit) {
        super(name);
        this.redisTemplate = redisTemplate;
        this.serializer = serializer;
        this.defaultExpire = defaultExpire;
        this.defaultTimeUnit = defaultTimeUnit;
    }

    /**
     * 生成Redis键
     *
     * @param key 原始键
     * @return Redis键
     */
    private String generateRedisKey(K key) {
        return name + ":" + key;
    }

    @Override
    public V get(K key) {
        Object value = redisTemplate.opsForValue().get(generateRedisKey(key));
        if (value == null) {
            return null;
        }
        return (V) serializer.deserialize(value);
    }

    @Override
    public void put(K key, V value, long expire, TimeUnit timeUnit) {
        String redisKey = generateRedisKey(key);
        Object serializedValue = serializer.serialize(value);
        
        if (expire > 0) {
            redisTemplate.opsForValue().set(redisKey, serializedValue, expire, timeUnit);
        } else {
            redisTemplate.opsForValue().set(redisKey, serializedValue, defaultExpire, defaultTimeUnit);
        }
    }

    @Override
    public boolean remove(K key) {
        return Boolean.TRUE.equals(redisTemplate.delete(generateRedisKey(key)));
    }

    @Override
    public void clear() {
        // 删除当前缓存名称下的所有键
        String pattern = name + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }

    @Override
    public boolean contains(K key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(generateRedisKey(key)));
    }

    @Override
    public long size() {
        String pattern = name + ":*";
        return redisTemplate.keys(pattern).size();
    }
} 