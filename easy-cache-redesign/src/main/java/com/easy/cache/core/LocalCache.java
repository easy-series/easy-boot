package com.easy.cache.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于内存的本地缓存实现
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public class LocalCache<K, V> extends AbstractCache<K, V> {

    /**
     * 默认初始容量
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * 默认最大容量
     */
    private static final int DEFAULT_MAXIMUM_SIZE = 10000;

    /**
     * 缓存数据存储
     */
    private final ConcurrentHashMap<K, CacheEntry<V>> store;

    /**
     * 缓存条目计数器
     */
    private final AtomicInteger size = new AtomicInteger(0);

    /**
     * 最大容量
     */
    private final int maximumSize;

    /**
     * 过期检查线程池
     */
    private static final ScheduledExecutorService EXPIRY_EXECUTOR = new ScheduledThreadPoolExecutor(1, r -> {
        Thread thread = new Thread(r, "LocalCacheExpiryThread");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 构造函数
     *
     * @param name 缓存名称
     */
    public LocalCache(String name) {
        this(name, DEFAULT_INITIAL_CAPACITY, DEFAULT_MAXIMUM_SIZE);
    }

    /**
     * 构造函数
     *
     * @param name            缓存名称
     * @param initialCapacity 初始容量
     * @param maximumSize     最大容量
     */
    public LocalCache(String name, int initialCapacity, int maximumSize) {
        super(name);
        this.store = new ConcurrentHashMap<>(initialCapacity);
        this.maximumSize = maximumSize;

        // 每60秒清理一次过期缓存
        EXPIRY_EXECUTOR.scheduleAtFixedRate(this::cleanExpired, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        CacheEntry<V> entry = store.get(key);
        if (entry == null) {
            return null;
        }

        // 检查是否过期
        if (entry.isExpired()) {
            remove(key);
            return null;
        }

        return entry.getValue();
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (key == null) {
            return;
        }

        // 检查容量
        if (size.get() >= maximumSize && !store.containsKey(key)) {
            return;
        }

        long expireAt = expireTime > 0
                ? System.currentTimeMillis() + timeUnit.toMillis(expireTime)
                : 0; // 0表示永不过期

        CacheEntry<V> oldEntry = store.put(key, new CacheEntry<>(value, expireAt));
        if (oldEntry == null) {
            size.incrementAndGet();
        }
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }

        CacheEntry<V> removed = store.remove(key);
        if (removed != null) {
            size.decrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        store.clear();
        size.set(0);
    }

    /**
     * 清理过期缓存
     */
    private void cleanExpired() {
        for (Map.Entry<K, CacheEntry<V>> entry : store.entrySet()) {
            if (entry.getValue().isExpired()) {
                remove(entry.getKey());
            }
        }
    }

    /**
     * 缓存条目，包含值和过期时间
     *
     * @param <V> 值类型
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long expireAt; // 过期时间戳，0表示永不过期

        CacheEntry(V value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        V getValue() {
            return value;
        }

        boolean isExpired() {
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        }
    }
}