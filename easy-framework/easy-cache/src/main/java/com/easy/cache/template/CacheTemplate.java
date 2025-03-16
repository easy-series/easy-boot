package com.easy.cache.template;

import java.time.Duration;

import com.easy.cache.api.CacheManager;
import org.springframework.stereotype.Component;

import com.easy.cache.api.Cache;
import com.easy.cache.config.CacheConfig;
import com.easy.cache.config.QuickConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存模板类，提供缓存操作的统一接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheTemplate {

    /**
     * 缓存管理器
     */
    private final CacheManager cacheManager;

    /**
     * 获取缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 缓存值
     */
    public <K, V> V get(String cacheName, K key) {
        Cache<K, V> cache = getCache(cacheName);
        return cache.get(key);
    }

    /**
     * 设置缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param <K>       键类型
     * @param <V>       值类型
     */
    public <K, V> void put(String cacheName, K key, V value) {
        Cache<K, V> cache = getCache(cacheName);
        cache.put(key, value);
    }

    /**
     * 设置缓存值，并指定过期时间
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param ttl       过期时间
     * @param <K>       键类型
     * @param <V>       值类型
     */
    public <K, V> void put(String cacheName, K key, V value, Duration ttl) {
        Cache<K, V> cache = getCache(cacheName);
        cache.put(key, value, ttl);
    }

    /**
     * 删除缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 是否成功删除
     */
    public <K, V> boolean remove(String cacheName, K key) {
        Cache<K, V> cache = getCache(cacheName);
        return cache.remove(key);
    }

    /**
     * 清空缓存
     *
     * @param cacheName 缓存名称
     * @param <K>       键类型
     * @param <V>       值类型
     */
    public <K, V> void clear(String cacheName) {
        Cache<K, V> cache = getCache(cacheName);
        cache.clear();
    }

    /**
     * 获取或计算缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param loader    缓存加载器
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 缓存值
     */
    public <K, V> V computeIfAbsent(String cacheName, K key, java.util.function.Function<K, V> loader) {
        Cache<K, V> cache = getCache(cacheName);
        return cache.computeIfAbsent(key, loader);
    }

    /**
     * 获取或计算缓存值，并指定过期时间
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param loader    缓存加载器
     * @param ttl       过期时间
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 缓存值
     */
    public <K, V> V computeIfAbsent(String cacheName, K key, java.util.function.Function<K, V> loader, Duration ttl) {
        Cache<K, V> cache = getCache(cacheName);
        return cache.computeIfAbsent(key, loader, ttl);
    }

    /**
     * 获取缓存实例
     *
     * @param cacheName 缓存名称
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return (Cache<K, V>) cacheManager.getCache(cacheName);
    }

    /**
     * 创建缓存实例
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 缓存实例
     */
    public <K, V> Cache<K, V> createCache(CacheConfig<K, V> config) {
        return cacheManager.getOrCreateCache(config);
    }

    /**
     * 使用快速配置创建缓存实例
     *
     * @param quickConfig 快速配置
     * @param <K>         键类型
     * @param <V>         值类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> createCache(QuickConfig quickConfig) {
        return (Cache<K, V>) cacheManager.getOrCreateCache(quickConfig);
    }

    /**
     * 尝试获取锁并执行操作
     *
     * @param cacheName 缓存名称
     * @param key       锁键
     * @param ttl       锁过期时间
     * @param action    要执行的操作
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 是否成功获取锁并执行操作
     */
    public <K, V> boolean tryLockAndRun(String cacheName, K key, Duration ttl, Runnable action) {
        Cache<K, V> cache = getCache(cacheName);
        return cache.tryLockAndRun(key, ttl, action);
    }
}