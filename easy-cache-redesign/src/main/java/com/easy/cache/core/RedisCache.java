package com.easy.cache.core;

import com.easy.cache.util.Serializer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的缓存实现
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public class RedisCache<K, V> extends AbstractCache<K, V> {

    /**
     * Redis模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 序列化器
     */
    private final Serializer serializer;

    /**
     * 默认过期时间
     */
    private long expireTime = 0;

    /**
     * 默认时间单位
     */
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 构造函数
     *
     * @param name          缓存名称
     * @param redisTemplate Redis模板
     * @param serializer    序列化器
     */
    public RedisCache(String name, RedisTemplate<String, Object> redisTemplate, Serializer serializer) {
        super(name);
        this.redisTemplate = redisTemplate;
        this.serializer = serializer;
    }

    /**
     * 设置默认过期时间
     *
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void setExpire(long expireTime, TimeUnit timeUnit) {
        this.expireTime = expireTime;
        this.timeUnit = timeUnit;
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
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (key == null) {
            return;
        }

        try {
            String redisKey = buildKey(key);
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();

            if (expireTime > 0) {
                ops.set(redisKey, value, expireTime, timeUnit);
            } else if (this.expireTime > 0) {
                ops.set(redisKey, value, this.expireTime, this.timeUnit);
            } else {
                ops.set(redisKey, value);
            }
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
            String pattern = name + ":*";
            Collection<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> redisKeys = new ArrayList<>(keys.size());
            Map<String, K> keyMapping = new HashMap<>(keys.size());

            for (K key : keys) {
                if (key != null) {
                    String redisKey = buildKey(key);
                    redisKeys.add(redisKey);
                    keyMapping.put(redisKey, key);
                }
            }

            if (redisKeys.isEmpty()) {
                return Collections.emptyMap();
            }

            List<Object> values = redisTemplate.opsForValue().multiGet(redisKeys);
            if (values == null) {
                return Collections.emptyMap();
            }

            Map<K, V> result = new HashMap<>(values.size());
            for (int i = 0; i < redisKeys.size(); i++) {
                if (i < values.size() && values.get(i) != null) {
                    K originalKey = keyMapping.get(redisKeys.get(i));
                    result.put(originalKey, (V) values.get(i));
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    @Override
    public void putAll(Map<K, V> map, long expireTime, TimeUnit timeUnit) {
        if (map == null || map.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> batch = new HashMap<>(map.size());
            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    String redisKey = buildKey(entry.getKey());
                    batch.put(redisKey, entry.getValue());
                }
            }

            if (batch.isEmpty()) {
                return;
            }

            redisTemplate.opsForValue().multiSet(batch);

            // 设置过期时间
            long actualExpireTime = expireTime > 0 ? expireTime : this.expireTime;
            TimeUnit actualTimeUnit = expireTime > 0 ? timeUnit : this.timeUnit;

            if (actualExpireTime > 0) {
                for (String redisKey : batch.keySet()) {
                    redisTemplate.expire(redisKey, actualExpireTime, actualTimeUnit);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    @Override
    public boolean removeAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return false;
        }

        try {
            List<String> redisKeys = new ArrayList<>(keys.size());
            for (K key : keys) {
                if (key != null) {
                    redisKeys.add(buildKey(key));
                }
            }

            if (redisKeys.isEmpty()) {
                return false;
            }

            Long count = redisTemplate.delete(redisKeys);
            return count != null && count > 0;
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            return false;
        }

        try {
            String redisKey = buildKey(key);
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 构建Redis键
     *
     * @param key 缓存键
     * @return Redis键
     */
    private String buildKey(K key) {
        return name + ":" + key.toString();
    }
}