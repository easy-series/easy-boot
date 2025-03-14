package com.easy.cache.core;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Redis缓存实现 (基于RedisTemplate)
 */
public class RedisCache<K, V> implements Cache<K, V> {

    /**
     * 序列化器接口
     */
    public interface Serializer {
        /**
         * 序列化对象
         */
        byte[] serialize(Object obj);

        /**
         * 反序列化对象
         */
        <T> T deserialize(byte[] bytes, Class<T> clazz);
    }

    private final String name;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Serializer serializer;
    private long expireTime = 0;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 构造函数
     */
    public RedisCache(String name, RedisTemplate<String, Object> redisTemplate, Serializer serializer) {
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.serializer = serializer;
    }

    /**
     * 设置过期时间
     */
    public void setExpire(long expireTime, TimeUnit timeUnit) {
        this.expireTime = expireTime;
        this.timeUnit = timeUnit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        try {
            String redisKey = buildKey(key);
            return (V) redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        V value = get(key);
        if (value == null && loader != null) {
            value = loader.apply(key);
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            return;
        }

        try {
            String redisKey = buildKey(key);
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();

            if (expireTime > 0) {
                ops.set(redisKey, value, expireTime, timeUnit);
            } else {
                ops.set(redisKey, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (key == null) {
            return;
        }

        try {
            String redisKey = buildKey(key);
            redisTemplate.opsForValue().set(redisKey, value, expireTime, timeUnit);
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }

        try {
            String redisKey = buildKey(key);
            return Boolean.TRUE.equals(redisTemplate.delete(redisKey));
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    @Override
    public void clear() {
        try {
            // 清空缓存需要谨慎，这里只清除与当前缓存名称相关的键
            String pattern = name + ":*";
            Collection<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 批量获取缓存
     * 
     * @param keys 缓存键集合
     * @return 缓存值映射
     */
    public Map<K, V> getAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<K, V> result = new HashMap<>();

        try {
            Map<String, K> keyMapping = new HashMap<>();
            for (K key : keys) {
                String redisKey = buildKey(key);
                keyMapping.put(redisKey, key);
            }

            // 批量获取
            Collection<String> redisKeys = keyMapping.keySet();
            if (!redisKeys.isEmpty()) {
                List<Object> values = redisTemplate.opsForValue().multiGet(redisKeys);
                if (values != null) {
                    int i = 0;
                    for (String redisKey : redisKeys) {
                        Object value = values.get(i++);
                        if (value != null) {
                            K originalKey = keyMapping.get(redisKey);
                            result.put(originalKey, (V) value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }

        return result;
    }

    /**
     * 批量放入缓存
     * 
     * @param map        缓存键值映射
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void putAll(Map<K, V> map, long expireTime, TimeUnit timeUnit) {
        if (map == null || map.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> entries = new HashMap<>();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                String redisKey = buildKey(entry.getKey());
                entries.put(redisKey, entry.getValue());
            }

            // 批量设置
            redisTemplate.opsForValue().multiSet(entries);

            // 设置过期时间
            if (expireTime > 0) {
                for (String redisKey : entries.keySet()) {
                    redisTemplate.expire(redisKey, expireTime, timeUnit);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 批量删除缓存
     * 
     * @param keys 缓存键集合
     * @return 是否成功
     */
    public boolean removeAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return false;
        }

        try {
            List<String> redisKeys = new ArrayList<>();
            for (K key : keys) {
                redisKeys.add(buildKey(key));
            }

            Long deleted = redisTemplate.delete(redisKeys);
            return deleted != null && deleted > 0;
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 构建Redis键
     */
    private String buildKey(K key) {
        return name + ":" + key.toString();
    }
}