package com.easy.cache.core.redis;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import com.easy.cache.core.AbstractCache;
import com.easy.cache.support.convertor.KeyConvertor;
import com.easy.cache.support.serializer.Serializer;
import com.easy.cache.support.stats.CacheStats;
import com.easy.cache.support.sync.CacheEvent;
import com.easy.cache.support.sync.CacheEventPublisher;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于Redis实现的远程缓存
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class RedisCache<K, V> extends AbstractCache<K, V> {

    /**
     * Redis模板
     */
    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * 值序列化器
     */
    private final Serializer serializer;

    /**
     * 缓存事件发布器
     */
    private final CacheEventPublisher eventPublisher;

    /**
     * 是否启用本地缓存同步
     */
    private final boolean syncLocal;

    /**
     * 构造方法
     *
     * @param name               缓存名称
     * @param keyConvertor       键转换器
     * @param redisTemplate      Redis模板
     * @param serializer         值序列化器
     * @param defaultExpiration  默认过期时间
     * @param loader             缓存加载器
     * @param cacheNullValues    是否缓存null值
     * @param penetrationProtect 是否启用缓存穿透保护
     * @param eventPublisher     缓存事件发布器
     * @param syncLocal          是否启用本地缓存同步
     */
    public RedisCache(String name, KeyConvertor keyConvertor, RedisTemplate<String, byte[]> redisTemplate,
            Serializer serializer, Duration defaultExpiration, Function<K, V> loader,
            boolean cacheNullValues, boolean penetrationProtect,
            CacheEventPublisher eventPublisher, boolean syncLocal) {
        super(name, keyConvertor, defaultExpiration, loader, cacheNullValues, penetrationProtect);
        this.redisTemplate = redisTemplate;
        this.serializer = serializer;
        this.eventPublisher = eventPublisher;
        this.syncLocal = syncLocal;
    }

    @Override
    public V get(K key) {
        String cacheKey = buildKey(key);
        ((CacheStats) stats).recordRequest();

        // 从Redis获取值
        byte[] bytes = redisTemplate.opsForValue().get(cacheKey);
        if (bytes != null) {
            ((CacheStats) stats).recordHit();
            return deserialize(bytes);
        }

        ((CacheStats) stats).recordMiss();

        // 如果未命中且有加载器，则加载值
        if (loader != null) {
            if (penetrationProtect) {
                // 使用缓存穿透保护机制加载值
                return loadValueWithProtection(key, cacheKey);
            } else {
                // 直接加载值
                V value = loadValue(key);
                if (value != null || cacheNullValues) {
                    put(key, value);
                }
                return value;
            }
        }

        return null;
    }

    /**
     * 使用缓存穿透保护机制加载值
     *
     * @param key      键
     * @param cacheKey 缓存键
     * @return 加载的值
     */
    private V loadValueWithProtection(K key, String cacheKey) {
        // 使用Redis实现分布式锁
        String lockKey = cacheKey + ":lock";
        if (tryLock(lockKey, Duration.ofSeconds(5))) {
            try {
                // 再次检查缓存，防止其他进程已经加载
                byte[] bytes = redisTemplate.opsForValue().get(cacheKey);
                if (bytes != null) {
                    ((CacheStats) stats).recordHit();
                    return deserialize(bytes);
                }

                // 加载值
                V value = loadValue(key);
                if (value != null || cacheNullValues) {
                    put(key, value);
                }
                return value;
            } finally {
                // 解锁
                redisTemplate.delete(lockKey);
            }
        } else {
            // 未获取到锁，等待一段时间后再次尝试获取缓存
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            byte[] bytes = redisTemplate.opsForValue().get(cacheKey);
            if (bytes != null) {
                ((CacheStats) stats).recordHit();
                return deserialize(bytes);
            }
            return null;
        }
    }

    /**
     * 加载值
     *
     * @param key 键
     * @return 加载的值
     */
    private V loadValue(K key) {
        if (loader == null) {
            return null;
        }

        long startTime = System.nanoTime();
        try {
            V value = loader.apply(key);
            ((CacheStats) stats).recordLoadSuccess();
            return value;
        } catch (Exception e) {
            ((CacheStats) stats).recordLoadFailure();
            log.error("加载缓存值失败: key={}", key, e);
            throw e;
        } finally {
            long loadTime = System.nanoTime() - startTime;
            ((CacheStats) stats).recordLoadTime(loadTime);
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, defaultExpiration);
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        if (value != null || cacheNullValues) {
            String cacheKey = buildKey(key);
            byte[] valueBytes = serialize(value);

            if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
                redisTemplate.opsForValue().set(cacheKey, valueBytes, ttl);
            } else {
                redisTemplate.opsForValue().set(cacheKey, valueBytes);
            }

            // 发布缓存更新事件
            publishUpdateEvent(cacheKey);
        }
    }

    @Override
    public boolean remove(K key) {
        String cacheKey = buildKey(key);
        boolean result = Boolean.TRUE.equals(redisTemplate.delete(cacheKey));

        // 发布缓存移除事件
        publishRemoveEvent(cacheKey);

        return result;
    }

    @Override
    public void clear() {
        // 查找所有以缓存名称为前缀的键
        Set<String> keys = redisTemplate.keys(name + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 发布缓存清空事件
        publishClearEvent();
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        String cacheKey = buildKey(key);
        ((CacheStats) stats).recordRequest();

        // 先尝试从缓存获取
        byte[] bytes = redisTemplate.opsForValue().get(cacheKey);
        if (bytes != null) {
            ((CacheStats) stats).recordHit();
            return deserialize(bytes);
        }

        // 使用提供的加载器加载值
        ((CacheStats) stats).recordMiss();
        long startTime = System.nanoTime();
        try {
            V value = loader.apply(key);
            ((CacheStats) stats).recordLoadSuccess();

            // 缓存值
            if (value != null || cacheNullValues) {
                byte[] valueBytes = serialize(value);
                if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.opsForValue().set(cacheKey, valueBytes, ttl);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, valueBytes);
                }

                // 发布缓存更新事件
                publishUpdateEvent(cacheKey);
            }

            return value;
        } catch (Exception e) {
            ((CacheStats) stats).recordLoadFailure();
            log.error("计算缓存值失败: key={}", key, e);
            throw e;
        } finally {
            long loadTime = System.nanoTime() - startTime;
            ((CacheStats) stats).recordLoadTime(loadTime);
        }
    }

    @Override
    public boolean tryLock(K key, Duration ttl) {
        String lockKey = buildKey(key) + ":lock";
        return tryLock(lockKey, ttl);
    }

    /**
     * 尝试获取Redis锁
     *
     * @param lockKey 锁键
     * @param ttl     锁过期时间
     * @return 是否成功获取锁
     */
    private boolean tryLock(String lockKey, Duration ttl) {
        // 使用Redis的NX命令实现分布式锁
        return Boolean.TRUE.equals(redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            // 设置锁，使用NX（不存在才设置）和PX（过期时间）选项
            return connection.set(
                    lockKey.getBytes(),
                    "1".getBytes(),
                    Expiration.from(ttl.toMillis(), TimeUnit.MILLISECONDS),
                    RedisStringCommands.SetOption.SET_IF_ABSENT);
        }));
    }

    @Override
    public void unlock(K key) {
        String lockKey = buildKey(key) + ":lock";
        redisTemplate.delete(lockKey);
    }

    /**
     * 发布缓存更新事件
     *
     * @param cacheKey 缓存键
     */
    private void publishUpdateEvent(String cacheKey) {
        if (syncLocal && eventPublisher != null) {
            CacheEvent event = CacheEvent.createUpdateEvent(name, cacheKey);
            try {
                eventPublisher.publish(event);
            } catch (Exception e) {
                log.error("发布缓存更新事件失败", e);
            }
        }
    }

    /**
     * 发布缓存移除事件
     *
     * @param cacheKey 缓存键
     */
    private void publishRemoveEvent(String cacheKey) {
        if (syncLocal && eventPublisher != null) {
            CacheEvent event = CacheEvent.createRemoveEvent(name, cacheKey);
            try {
                eventPublisher.publish(event);
            } catch (Exception e) {
                log.error("发布缓存移除事件失败", e);
            }
        }
    }

    /**
     * 发布缓存清空事件
     */
    private void publishClearEvent() {
        if (syncLocal && eventPublisher != null) {
            CacheEvent event = CacheEvent.createClearEvent(name);
            try {
                eventPublisher.publish(event);
            } catch (Exception e) {
                log.error("发布缓存清空事件失败", e);
            }
        }
    }

    /**
     * 序列化值
     *
     * @param value 值
     * @return 序列化后的字节数组
     */
    private byte[] serialize(V value) {
        if (value == null) {
            // 对空值进行特殊处理
            return new byte[0];
        }
        return serializer.serialize(value);
    }

    /**
     * 反序列化值
     *
     * @param bytes 字节数组
     * @return 反序列化后的值
     */
    @SuppressWarnings("unchecked")
    private V deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            // 空字节数组表示空值
            return null;
        }
        return (V) serializer.deserialize(bytes, Object.class);
    }

    public long size() {
        // 无法精确获取，返回-1
        return -1;
    }
}