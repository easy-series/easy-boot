package com.easy.cache.core;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import com.easy.cache.api.Cache;
import com.easy.cache.stats.DefaultCacheStats;
import com.easy.cache.support.convertor.KeyConvertor;
import com.easy.cache.support.stats.CacheStats;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存抽象类，提供基础实现
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    /**
     * 缓存名称
     */
    @Getter
    protected final String name;

    /**
     * 键转换器
     */
    protected final KeyConvertor keyConvertor;

    /**
     * 默认过期时间
     */
    protected final Duration defaultExpiration;

    /**
     * 缓存加载器
     */
    protected final Function<K, V> loader;

    /**
     * 是否缓存null值
     */
    protected final boolean cacheNullValues;

    /**
     * 是否启用穿透保护
     */
    protected final boolean penetrationProtect;

    /**
     * 缓存统计信息
     */
    protected final DefaultCacheStats stats;

    /**
     * 异步操作执行器
     */
    protected final Executor executor;

    /**
     * 构造方法
     *
     * @param name               缓存名称
     * @param keyConvertor       键转换器
     * @param defaultExpiration  默认过期时间
     * @param loader             缓存加载器
     * @param cacheNullValues    是否缓存null值
     * @param penetrationProtect 是否启用穿透保护
     */
    protected AbstractCache(String name, KeyConvertor keyConvertor, Duration defaultExpiration,
            Function<K, V> loader, boolean cacheNullValues, boolean penetrationProtect) {
        this.name = name;
        this.keyConvertor = keyConvertor;
        this.defaultExpiration = defaultExpiration;
        this.loader = loader;
        this.cacheNullValues = cacheNullValues;
        this.penetrationProtect = penetrationProtect;
        this.stats = new DefaultCacheStats();
        this.executor = ForkJoinPool.commonPool();
    }

    @Override
    public String getName() {
        return name;
    }

    public CacheStats getStats() {
        return stats;
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader) {
        return computeIfAbsent(key, loader, defaultExpiration);
    }

    public boolean evict(K key) {
        return remove(key);
    }

    @Override
    public CompletableFuture<V> getAsync(K key) {
        return CompletableFuture.supplyAsync(() -> get(key), executor);
    }

    @Override
    public CompletableFuture<Void> putAsync(K key, V value) {
        return CompletableFuture.runAsync(() -> put(key, value), executor);
    }

    @Override
    public boolean tryLockAndRun(K key, Duration ttl, Runnable action) {
        if (tryLock(key, ttl)) {
            try {
                action.run();
                return true;
            } finally {
                // 尝试解锁，实际需要具体实现类完成
                try {
                    unlock(key);
                } catch (Exception e) {
                    log.warn("解锁失败: key={}", key, e);
                }
            }
        }
        return false;
    }

    /**
     * 解锁操作，由子类实现
     *
     * @param key 锁键
     */
    public abstract void unlock(K key);

    /**
     * 构建缓存键
     *
     * @param key 原始键
     * @return 构建后的缓存键
     */
    protected String buildKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("缓存键不能为null");
        }
        return name + ":" + keyConvertor.convert(key);
    }
}