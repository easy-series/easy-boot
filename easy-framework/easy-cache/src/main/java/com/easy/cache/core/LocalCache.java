package com.easy.cache.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的本地缓存实现
 */
public class LocalCache<K, V> extends AbstractCache<K, V> {

    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor;
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    public LocalCache(String name) {
        super(name);
        this.executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "LocalCache-Expiry-" + name);
            thread.setDaemon(true);
            return thread;
        });

        // 定期清理过期缓存
        this.executor.scheduleWithFixedDelay(this::cleanExpiredEntries, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public V get(K key) {
        checkKey(key);
        CacheEntry<V> entry = cache.get(key);

        if (entry == null) {
            missCount.incrementAndGet();
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            missCount.incrementAndGet();
            return null;
        }

        hitCount.incrementAndGet();
        return entry.getValue();
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        checkKey(key);
        if (value == null) {
            return;
        }

        long expireTimeMillis = expireTime <= 0 ? 0 : System.currentTimeMillis() + timeUnit.toMillis(expireTime);
        cache.put(key, new CacheEntry<>(value, expireTimeMillis));
    }

    @Override
    public boolean remove(K key) {
        checkKey(key);
        return cache.remove(key) != null;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    /**
     * 获取缓存命中率
     */
    public double getHitRate() {
        long hits = hitCount.get();
        long total = hits + missCount.get();
        return total == 0 ? 0 : (double) hits / total;
    }

    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }

    /**
     * 清理过期缓存项
     */
    private void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        cache.entrySet()
                .removeIf(entry -> entry.getValue().getExpireTime() > 0 && entry.getValue().getExpireTime() <= now);
    }

    /**
     * 缓存项，包含值和过期时间
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long expireTime;

        public CacheEntry(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public V getValue() {
            return value;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }
    }
}