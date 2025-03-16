package com.easy.cache.core;

import com.easy.cache.config.QuickConfig;
import com.easy.cache.exception.CacheException;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存操作模板类，提供便捷的缓存操作方法
 */
@Component
public class CacheTemplate {

    /**
     * 缓存管理器
     */
    private final CacheManager cacheManager;

    /**
     * 缓存实例缓存，避免重复创建
     */
    private final Map<String, Cache<?, ?>> cacheMap = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * 
     * @param cacheManager 缓存管理器
     */
    public CacheTemplate(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 获取缓存实例
     * 
     * @param name 缓存名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) cacheMap.computeIfAbsent(name, k -> {
            Cache<K, V> cache = cacheManager.getCache(name);
            if (cache == null) {
                throw new CacheException("找不到名为 '" + name + "' 的缓存");
            }
            return cache;
        });
    }

    /**
     * 获取或创建缓存
     * 
     * @param config 快速配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(QuickConfig config) {
        String name = config.getName();
        return (Cache<K, V>) cacheMap.computeIfAbsent(name, k -> cacheManager.getOrCreateCache(config));
    }

    /**
     * 获取值
     * 
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 缓存值
     */
    public <K, V> V get(String cacheName, K key) {
        return getCache(cacheName).get(key);
    }

    /**
     * 获取值，不存在则计算并存储
     * 
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param loader    值加载器
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 缓存值
     */
    public <K, V> V computeIfAbsent(String cacheName, K key, Function<K, V> loader) {
        return getCache(cacheName).computeIfAbsent(key, loader);
    }

    /**
     * 存储值
     * 
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param <K>       键类型
     * @param <V>       值类型
     */
    public <K, V> void put(String cacheName, K key, V value) {
        getCache(cacheName).put(key, value);
    }

    /**
     * 存储值并设置过期时间
     * 
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param expire    过期时间
     * @param timeUnit  时间单位
     * @param <K>       键类型
     * @param <V>       值类型
     */
    public <K, V> void put(String cacheName, K key, V value, long expire, TimeUnit timeUnit) {
        getCache(cacheName).put(key, value, expire, timeUnit);
    }

    /**
     * 移除缓存项
     * 
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param <K>       键类型
     * @return 是否成功移除
     */
    public <K> boolean remove(String cacheName, K key) {
        return getCache(cacheName).remove(key);
    }

    /**
     * 创建一个新的缓存并返回
     * 
     * @param name      缓存名称
     * @param expire    过期时间
     * @param cacheType 缓存类型
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 缓存实例
     */
    public <K, V> Cache<K, V> createCache(String name, Duration expire, CacheType cacheType) {
        QuickConfig config = QuickConfig.newBuilder(name)
                .expire(expire)
                .cacheType(cacheType)
                .build();
        return getOrCreateCache(config);
    }

    /**
     * 尝试获取锁并执行操作
     * 
     * @param cacheName 缓存名称
     * @param key       锁键
     * @param ttl       锁持有时间
     * @param unit      时间单位
     * @param action    执行的操作
     * @param <K>       键类型
     */
    public <K> void tryLockAndRun(String cacheName, K key, long ttl, TimeUnit unit, Runnable action) {
        getCache(cacheName).tryLockAndRun(key, ttl, unit, action);
    }
}