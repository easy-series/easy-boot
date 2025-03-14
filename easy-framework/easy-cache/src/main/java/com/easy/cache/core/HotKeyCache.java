package com.easy.cache.core;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 热点缓存实现，用于防止缓存击穿
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class HotKeyCache<K, V> extends AbstractCache<K, V> {

    /**
     * 被装饰的缓存
     */
    private final Cache<K, V> delegate;

    /**
     * 热点键计数器
     */
    private final com.github.benmanes.caffeine.cache.Cache<K, AtomicInteger> hotKeyCounter;

    /**
     * 热点键本地缓存
     */
    private final com.github.benmanes.caffeine.cache.Cache<K, V> hotKeyLocalCache;

    /**
     * 热点阈值
     */
    private final int threshold;

    /**
     * 时间窗口（毫秒）
     */
    private final long windowInMillis;

    /**
     * 热点键本地缓存时间（秒）
     */
    private final int localCacheSeconds;

    /**
     * 构造函数
     *
     * @param delegate 被装饰的缓存
     * @param threshold 热点阈值
     * @param windowInMillis 时间窗口（毫秒）
     * @param localCacheSeconds 热点键本地缓存时间（秒）
     */
    public HotKeyCache(Cache<K, V> delegate, int threshold, long windowInMillis, int localCacheSeconds) {
        super(delegate.getName() + "_hot_key");
        this.delegate = delegate;
        this.threshold = threshold;
        this.windowInMillis = windowInMillis;
        this.localCacheSeconds = localCacheSeconds;

        this.hotKeyCounter = Caffeine.newBuilder()
                .expireAfterWrite(windowInMillis, TimeUnit.MILLISECONDS)
                .build();

        this.hotKeyLocalCache = Caffeine.newBuilder()
                .expireAfterWrite(localCacheSeconds, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public V get(K key) {
        // 先检查热点键本地缓存
        V localValue = hotKeyLocalCache.getIfPresent(key);
        if (localValue != null) {
            return localValue;
        }

        // 增加热点计数
        AtomicInteger counter = hotKeyCounter.get(key, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        // 从委托缓存获取值
        V value = delegate.get(key);

        // 如果超过热点阈值，加入本地缓存
        if (count >= threshold && value != null) {
            hotKeyLocalCache.put(key, value);
        }

        return value;
    }

    @Override
    public void put(K key, V value, long expire, TimeUnit timeUnit) {
        // 更新委托缓存
        delegate.put(key, value, expire, timeUnit);

        // 检查是否为热点键
        AtomicInteger counter = hotKeyCounter.getIfPresent(key);
        if (counter != null && counter.get() >= threshold) {
            // 更新热点键本地缓存
            hotKeyLocalCache.put(key, value);
        }
    }

    @Override
    public boolean remove(K key) {
        // 从热点键本地缓存移除
        hotKeyLocalCache.invalidate(key);
        // 从热点键计数器移除
        hotKeyCounter.invalidate(key);
        // 从委托缓存移除
        return delegate.remove(key);
    }

    @Override
    public void clear() {
        hotKeyLocalCache.invalidateAll();
        hotKeyCounter.invalidateAll();
        delegate.clear();
    }

    @Override
    public boolean contains(K key) {
        // 先检查热点键本地缓存
        if (hotKeyLocalCache.getIfPresent(key) != null) {
            return true;
        }
        // 再检查委托缓存
        return delegate.contains(key);
    }

    @Override
    public long size() {
        return delegate.size();
    }
} 