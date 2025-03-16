package com.easy.cache.core;

import com.easy.cache.exception.CacheException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存接口，定义缓存的基本操作
 * 
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public interface Cache<K, V> {

    /**
     * 获取缓存名称
     * 
     * @return 缓存名称
     */
    String getName();

    /**
     * 根据键获取值
     * 
     * @param key 缓存键
     * @return 缓存值，如果不存在则返回null
     */
    V get(K key);

    /**
     * 如果缓存中不存在，则计算并保存，然后返回
     * 
     * @param key 缓存键
     * @param loader 值加载器，用于计算值
     * @return 缓存值
     */
    V computeIfAbsent(K key, Function<K, V> loader);

    /**
     * 放入缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(K key, V value);

    /**
     * 放入缓存并设置过期时间
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param expire 过期时间
     * @param timeUnit 时间单位
     */
    void put(K key, V value, long expire, TimeUnit timeUnit);

    /**
     * 删除缓存
     * 
     * @param key 缓存键
     * @return 是否删除成功
     */
    boolean remove(K key);

    /**
     * 检查缓存中是否包含指定键
     * 
     * @param key 缓存键
     * @return 是否包含
     */
    boolean containsKey(K key);

    /**
     * 获取缓存结果（包含更多元数据，支持异步操作）
     * 
     * @param key 缓存键
     * @return 缓存获取结果
     */
    CacheGetResult<V> GET(K key);

    /**
     * 异步获取缓存值
     * 
     * @param key 缓存键
     * @return 包含缓存值的Future
     */
    CompletableFuture<ResultData<V>> getAsync(K key);

    /**
     * 尝试获取锁并执行操作
     * 
     * @param key 锁键
     * @param ttl 锁持有时间
     * @param unit 时间单位
     * @param action 获取锁后执行的操作
     * @throws CacheException 获取锁失败或执行操作时发生异常
     */
    void tryLockAndRun(K key, long ttl, TimeUnit unit, Runnable action) throws CacheException;

    /**
     * 清空缓存
     */
    void clear();
} 