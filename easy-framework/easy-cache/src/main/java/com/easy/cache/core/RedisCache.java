package com.easy.cache.core;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Redis缓存实现
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
    private final JedisPool jedisPool;
    private final Serializer serializer;
    private long expireTime = 0;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 构造函数
     */
    public RedisCache(String name, JedisPool jedisPool, Serializer serializer) {
        this.name = name;
        this.jedisPool = jedisPool;
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

        int retries = 3;
        Exception lastException = null;
        
        while (retries > 0) {
            try (Jedis jedis = jedisPool.getResource()) {
                byte[] keyBytes = serializer.serialize(key.toString());
                byte[] valueBytes = jedis.get(keyBytes);
                if (valueBytes == null) {
                    return null;
                }
                @SuppressWarnings("unchecked")
                V value = (V) serializer.deserialize(valueBytes, Object.class);
                return value;
            } catch (JedisConnectionException e) {
                lastException = e;
                retries--;
                if (retries > 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                }
            }
        }
        
        if (lastException != null) {
            throw new RuntimeException("Redis连接失败", lastException);
        }
        
        return null;
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

        try (Jedis jedis = jedisPool.getResource()) {
            byte[] keyBytes = serializer.serialize(key.toString());
            byte[] valueBytes = serializer.serialize(value);

            if (expireTime > 0) {
                long seconds = timeUnit.toSeconds(expireTime);
                jedis.setex(keyBytes, (int) seconds, valueBytes);
            } else {
                jedis.set(keyBytes, valueBytes);
            }
        }
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (key == null) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            byte[] keyBytes = serializer.serialize(key.toString());
            byte[] valueBytes = serializer.serialize(value);

            long seconds = timeUnit.toSeconds(expireTime);
            jedis.setex(keyBytes, (int) seconds, valueBytes);
        }
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            byte[] keyBytes = serializer.serialize(key.toString());
            return jedis.del(keyBytes) > 0;
        }
    }

    @Override
    public void clear() {
        // 清空缓存需要谨慎，这里只清除与当前缓存名称相关的键
        try (Jedis jedis = jedisPool.getResource()) {
            String pattern = name + ":*";
            byte[] patternBytes = serializer.serialize(pattern);
            for (byte[] keyBytes : jedis.keys(patternBytes)) {
                jedis.del(keyBytes);
            }
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
        
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            Map<K, Response<byte[]>> responses = new HashMap<>();
            
            for (K key : keys) {
                byte[] keyBytes = serializer.serialize(key.toString());
                responses.put(key, pipeline.get(keyBytes));
            }
            
            pipeline.sync();
            
            for (Map.Entry<K, Response<byte[]>> entry : responses.entrySet()) {
                byte[] valueBytes = entry.getValue().get();
                if (valueBytes != null) {
                    @SuppressWarnings("unchecked")
                    V value = (V) serializer.deserialize(valueBytes, Object.class);
                    result.put(entry.getKey(), value);
                }
            }
        }
        
        return result;
    }

    /**
     * 批量放入缓存
     * 
     * @param map 缓存键值映射
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     */
    public void putAll(Map<K, V> map, long expireTime, TimeUnit timeUnit) {
        if (map == null || map.isEmpty()) {
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            
            for (Map.Entry<K, V> entry : map.entrySet()) {
                byte[] keyBytes = serializer.serialize(entry.getKey().toString());
                byte[] valueBytes = serializer.serialize(entry.getValue());
                
                if (expireTime > 0) {
                    long seconds = timeUnit.toSeconds(expireTime);
                    pipeline.setex(keyBytes, (int) seconds, valueBytes);
                } else {
                    pipeline.set(keyBytes, valueBytes);
                }
            }
            
            pipeline.sync();
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
        
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            
            for (K key : keys) {
                byte[] keyBytes = serializer.serialize(key.toString());
                pipeline.del(keyBytes);
            }
            
            pipeline.sync();
            return true;
        }
    }
}