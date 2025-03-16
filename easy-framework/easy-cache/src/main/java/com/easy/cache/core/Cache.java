package com.easy.cache.core;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.easy.cache.support.stats.CacheStats;

/**
 * 缓存接口，定义缓存的基本操作
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface Cache<K, V> {

    /**
     * 获取缓存名称
     *
     * @return 缓存名称
     */
    String getName();

    /**
     * 根据键获取缓存中的值
     *
     * @param key 键
     * @return 值，如果不存在返回null
     */
    V get(K key);

    /**
     * 将键值对放入缓存
     *
     * @param key   键
     * @param value 值
     */
    void put(K key, V value);

    /**
     * 将键值对放入缓存，并指定过期时间
     *
     * @param key   键
     * @param value 值
     * @param ttl   过期时间
     */
    void put(K key, V value, Duration ttl);

    /**
     * 如果缓存中不存在该键，则使用提供的函数计算并放入缓存
     *
     * @param key    键
     * @param loader 值加载函数
     * @return 计算的值或缓存中已存在的值
     */
    V computeIfAbsent(K key, Function<K, V> loader);

    /**
     * 如果缓存中不存在该键，则使用提供的函数计算并放入缓存，并指定过期时间
     *
     * @param key    键
     * @param loader 值加载函数
     * @param ttl    过期时间
     * @return 计算的值或缓存中已存在的值
     */
    V computeIfAbsent(K key, Function<K, V> loader, Duration ttl);

    /**
     * 从缓存中移除指定键的值
     *
     * @param key 键
     * @return 是否成功移除
     */
    boolean remove(K key);

    /**
     * 驱逐缓存中的指定键
     *
     * @param key 键
     * @return 是否成功驱逐
     */
    boolean evict(K key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    CacheStats getStats();

    /**
     * 获取缓存中的条目数量
     *
     * @return 条目数量
     */
    long size();

    /**
     * 异步获取缓存值
     *
     * @param key 键
     * @return 包含值的CompletableFuture
     */
    CompletableFuture<V> getAsync(K key);

    /**
     * 异步设置缓存
     *
     * @param key   键
     * @param value 值
     * @return 表示操作完成的CompletableFuture
     */
    CompletableFuture<Void> putAsync(K key, V value);

    /**
     * 尝试获取锁
     *
     * @param key 锁键
     * @param ttl 锁的存活时间
     * @return 如果成功获取锁返回true，否则返回false
     */
    boolean tryLock(K key, Duration ttl);

    /**
     * 尝试获取锁并执行操作
     *
     * @param key    锁键
     * @param ttl    锁过期时间
     * @param action 要执行的操作
     * @return 是否成功获取锁并执行操作
     */
    boolean tryLockAndRun(K key, Duration ttl, Runnable action);

    /**
     * 释放锁
     *
     * @param key 锁键
     */
    void unlock(K key);
}