package com.easy.cache.core;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存实现，基于Caffeine Cache
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class LocalCache<K, V> extends AbstractCache<K, V> {

    /**
     * Caffeine缓存实例
     */
    private final com.github.benmanes.caffeine.cache.Cache<K, V> cache;

    /**
     * 构造函数
     *
     * @param name 缓存名称
     * @param initialCapacity 初始容量
     * @param maximumSize 最大容量
     * @param expireAfterWrite 写入后过期时间
     * @param timeUnit 时间单位
     */
    public LocalCache(String name, int initialCapacity, long maximumSize, long expireAfterWrite, TimeUnit timeUnit) {
        super(name);

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .maximumSize(maximumSize);

        if (expireAfterWrite > 0) {
            builder.expireAfterWrite(expireAfterWrite, timeUnit);
        }

        this.cache = builder.build();
    }

    /**
     * 简单构造函数
     *
     * @param name 缓存名称
     */
    public LocalCache(String name) {
        this(name, 100, 10000, 30, TimeUnit.MINUTES);
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value, long expire, TimeUnit timeUnit) {
        cache.put(key, value);
    }

    @Override
    public boolean remove(K key) {
        V previous = cache.getIfPresent(key);
        if (previous != null) {
            cache.invalidate(key);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }
} 