package com.easy.cache.local.caffeine;

import com.easy.cache.config.CacheConfig;
import com.easy.cache.core.AbstractCache;
import com.easy.cache.support.NullValue;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * 基于Caffeine的本地缓存实现
 * 
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class CaffeineCache<K, V> extends AbstractCache<K, V> {

    /**
     * Caffeine缓存实例
     */
    private final Cache<K, Object> caffeineCache;

    /**
     * 构造函数
     * 
     * @param name   缓存名称
     * @param config 缓存配置
     */
    public CaffeineCache(String name, CacheConfig<K, V> config) {
        super(name, config);

        // 构建Caffeine缓存
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .initialCapacity(16)
                .maximumSize(config.getLocalLimit());

        // 设置过期时间
        if (config.getExpireAfterWrite() > 0) {
            builder.expireAfterWrite(config.getExpireAfterWrite(), config.getTimeUnit());
        }

        // 创建缓存实例
        caffeineCache = builder.build();
    }

    @Override
    public V get(K key) {
        Object value = caffeineCache.getIfPresent(key);

        if (value == null) {
            stats.recordHit(false);
            return null;
        }

        stats.recordHit(true);
        return processValue(value);
    }

    @Override
    public V computeIfAbsent(K key, java.util.function.Function<K, V> loader) {
        long startTime = System.currentTimeMillis();
        try {
            // 利用Caffeine的computeIfAbsent方法
            Object value = caffeineCache.get(key, k -> {
                V loadedValue = loader.apply(k);
                if (loadedValue == null && config.isCacheNullValue()) {
                    return NullValue.INSTANCE;
                }
                return loadedValue;
            });

            boolean success = value != null;
            long loadTime = System.currentTimeMillis() - startTime;
            stats.recordLoad(success, loadTime);

            return processValue(value);
        } catch (Exception e) {
            stats.recordError();
            throw e;
        }
    }

    @Override
    public void put(K key, V value) {
        if (value == null) {
            if (config.isCacheNullValue()) {
                caffeineCache.put(key, NullValue.INSTANCE);
            } else {
                caffeineCache.invalidate(key);
            }
        } else {
            caffeineCache.put(key, value);
        }
    }

    @Override
    public boolean remove(K key) {
        // Caffeine没有返回值的remove方法，不能知道是否真正删除了元素
        boolean existsBeforeRemove = caffeineCache.getIfPresent(key) != null;
        caffeineCache.invalidate(key);
        return existsBeforeRemove;
    }

    @Override
    public boolean containsKey(K key) {
        return caffeineCache.getIfPresent(key) != null;
    }

    @Override
    public void clear() {
        caffeineCache.invalidateAll();
    }

    /**
     * 获取缓存大小估计值
     * 
     * @return 缓存大小
     */
    public long size() {
        return caffeineCache.estimatedSize();
    }

    /**
     * 清理过期的缓存项
     */
    public void cleanUp() {
        caffeineCache.cleanUp();
    }
}