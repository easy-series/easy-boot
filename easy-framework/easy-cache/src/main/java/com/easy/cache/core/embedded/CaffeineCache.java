package com.easy.cache.core.embedded;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import com.easy.cache.core.AbstractCache;
import com.easy.cache.support.convertor.KeyConvertor;
import com.easy.cache.support.stats.CacheStats;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于Caffeine实现的本地缓存
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class CaffeineCache<K, V> extends AbstractCache<K, V> implements LocalCache<K, V> {

    /**
     * Caffeine缓存实例
     */
    private final Cache<String, V> cache;

    /**
     * 防止缓存穿透的锁映射
     */
    private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param name               缓存名称
     * @param keyConvertor       键转换器
     * @param expireAfterWrite   写入后过期时间
     * @param expireAfterAccess  访问后过期时间
     * @param refreshAfterWrite  写入后刷新时间
     * @param initialCapacity    初始容量
     * @param maximumSize        最大容量
     * @param recordStats        是否记录统计信息
     * @param loader             加载器函数
     * @param cacheNullValues    是否缓存空值
     * @param penetrationProtect 是否启用缓存穿透保护
     */
    public CaffeineCache(String name, KeyConvertor keyConvertor,
            Duration expireAfterWrite, Duration expireAfterAccess, Duration refreshAfterWrite,
            int initialCapacity, long maximumSize, boolean recordStats,
            Function<K, V> loader, boolean cacheNullValues, boolean penetrationProtect) {
        super(name, keyConvertor, expireAfterWrite, loader, cacheNullValues, penetrationProtect);

        // 构建Caffeine缓存
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .initialCapacity(initialCapacity);

        // 设置最大容量
        if (maximumSize > 0) {
            builder.maximumSize(maximumSize);
        }

        // 设置过期策略
        if (expireAfterWrite != null && !expireAfterWrite.isNegative() && !expireAfterWrite.isZero()) {
            builder.expireAfterWrite(expireAfterWrite);
        }
        if (expireAfterAccess != null && !expireAfterAccess.isNegative() && !expireAfterAccess.isZero()) {
            builder.expireAfterAccess(expireAfterAccess);
        }
        if (refreshAfterWrite != null && !refreshAfterWrite.isNegative() && !refreshAfterWrite.isZero()) {
            builder.refreshAfterWrite(refreshAfterWrite);
        }

        // 启用统计
        if (recordStats) {
            builder.recordStats();
        }

        this.cache = builder.build();
    }

    @Override
    public V get(K key) {
        String cacheKey = buildKey(key);
        ((CacheStats) stats).recordRequest();

        V value = cache.getIfPresent(cacheKey);
        if (value != null) {
            ((CacheStats) stats).recordHit();
            return value;
        }

        ((CacheStats) stats).recordMiss();

        // 如果未命中且有加载器，则加载值
        if (loader != null) {
            if (penetrationProtect) {
                // 使用缓存穿透保护机制加载值
                return loadValueWithProtection(key, cacheKey);
            } else {
                // 直接加载值
                V loadedValue = loadValue(key);
                if (loadedValue != null || cacheNullValues) {
                    put(key, loadedValue);
                }
                return loadedValue;
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
        Lock lock = lockMap.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        boolean locked = false;
        try {
            // 尝试获取锁，最多等待100毫秒
            locked = lock.tryLock(100, TimeUnit.MILLISECONDS);
            if (locked) {
                // 再次检查缓存，防止其他线程已经加载
                V value = cache.getIfPresent(cacheKey);
                if (value != null) {
                    ((CacheStats) stats).recordHit();
                    return value;
                }

                // 加载值
                V loadedValue = loadValue(key);
                if (loadedValue != null || cacheNullValues) {
                    put(key, loadedValue);
                }
                return loadedValue;
            } else {
                // 未获取到锁，等待一段时间后再次尝试获取缓存
                Thread.sleep(50);
                V value = cache.getIfPresent(cacheKey);
                if (value != null) {
                    ((CacheStats) stats).recordHit();
                    return value;
                }
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (locked) {
                lock.unlock();
                // 如果锁不再需要，则从映射中移除
                lockMap.remove(cacheKey);
            }
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
        if (value != null || cacheNullValues) {
            String cacheKey = buildKey(key);
            cache.put(cacheKey, value);
        }
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        // Caffeine不支持单个键的过期时间
        // 这里简单实现，忽略ttl参数
        put(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        String cacheKey = buildKey(key);
        ((CacheStats) stats).recordRequest();

        // 先尝试从缓存获取
        V value = cache.getIfPresent(cacheKey);
        if (value != null) {
            ((CacheStats) stats).recordHit();
            return value;
        }

        // 使用提供的加载器加载值
        ((CacheStats) stats).recordMiss();
        long startTime = System.nanoTime();
        try {
            value = loader.apply(key);
            ((CacheStats) stats).recordLoadSuccess();

            // 缓存值
            if (value != null || cacheNullValues) {
                cache.put(cacheKey, value);
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
    public boolean remove(K key) {
        String cacheKey = buildKey(key);
        boolean keyExists = cache.getIfPresent(cacheKey) != null;
        cache.invalidate(cacheKey);
        return keyExists;
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }

    @Override
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    @Override
    public boolean tryLock(K key, Duration ttl) {
        String lockKey = buildKey(key) + ":lock";
        Lock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        try {
            return lock.tryLock(ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(K key) {
        String lockKey = buildKey(key) + ":lock";
        Lock lock = lockMap.get(lockKey);
        if (lock != null) {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                // 忽略，说明当前线程不是锁的持有者
                log.warn("尝试释放未持有的锁: key={}", key);
            } finally {
                lockMap.remove(lockKey);
            }
        }
    }
}