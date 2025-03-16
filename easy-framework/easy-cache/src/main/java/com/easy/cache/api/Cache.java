package com.easy.cache.api;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 缓存接口，定义缓存的基本操作
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface Cache<K, V> {
    
    /**
     * 根据键获取值
     *
     * @param key 键
     * @return 值，如果不存在则返回null
     */
    V get(K key);
    
    /**
     * 设置缓存
     *
     * @param key 键
     * @param value 值
     */
    void put(K key, V value);
    
    /**
     * 设置缓存，并指定过期时间
     *
     * @param key 键
     * @param value 值
     * @param ttl 过期时间
     */
    void put(K key, V value, Duration ttl);
    
    /**
     * 删除缓存
     *
     * @param key 键
     * @return 如果键存在并被移除则返回true，否则返回false
     */
    boolean remove(K key);
    
    /**
     * 清空缓存
     */
    void clear();
    
    /**
     * 如果键不存在，则计算并设置值
     *
     * @param key 键
     * @param loader 值加载器
     * @return 值
     */
    V computeIfAbsent(K key, Function<K, V> loader);
    
    /**
     * 如果键不存在，则计算并设置值，并指定过期时间
     *
     * @param key 键
     * @param loader 值加载器
     * @param ttl 过期时间
     * @return 值
     */
    V computeIfAbsent(K key, Function<K, V> loader, Duration ttl);
    
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
     * @param key 键
     * @param value 值
     * @return 表示操作完成的CompletableFuture
     */
    CompletableFuture<Void> putAsync(K key, V value);
    
    /**
     * 尝试获取锁
     *
     * @param key 锁键
     * @param ttl 锁过期时间
     * @return 是否成功获取锁
     */
    boolean tryLock(K key, Duration ttl);
    
    /**
     * 尝试获取锁并执行操作
     *
     * @param key 锁键
     * @param ttl 锁过期时间
     * @param action 要执行的操作
     * @return 是否成功获取锁并执行操作
     */
    boolean tryLockAndRun(K key, Duration ttl, Runnable action);
    
    /**
     * 获取缓存名称
     *
     * @return 缓存名称
     */
    String getName();
} 