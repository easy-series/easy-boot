package com.easy.cache.core;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
     * 从缓存中获取值
     * 
     * @param key 缓存键
     * @return 缓存值，如果不存在返回null
     */
    V get(K key);
    
    /**
     * 将值放入缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(K key, V value);
    
    /**
     * 将值放入缓存，并设置过期时间
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param expire 过期时间
     * @param timeUnit 时间单位
     */
    void put(K key, V value, long expire, TimeUnit timeUnit);
    
    /**
     * 将值放入缓存，并设置过期时间
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param duration 过期时间
     */
    default void put(K key, V value, Duration duration) {
        put(key, value, duration.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * 从缓存中删除值
     * 
     * @param key 缓存键
     * @return 如果值存在并删除成功，返回true
     */
    boolean remove(K key);
    
    /**
     * 清空缓存
     */
    void clear();
    
    /**
     * 判断缓存中是否存在指定键
     * 
     * @param key 缓存键
     * @return 如果存在返回true
     */
    boolean contains(K key);
    
    /**
     * 获取缓存中的项目数量
     * 
     * @return 缓存项目数量
     */
    long size();
} 