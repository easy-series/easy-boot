package com.easy.cache.core;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存接口，定义基本的缓存操作
 */
public interface Cache<K, V> {

    /**
     * 从缓存中获取值
     * 
     * @param key 缓存键
     * @return 缓存值，如果不存在则返回null
     */
    V get(K key);

    /**
     * 从缓存中获取值，如果不存在则使用loader加载并缓存
     * 
     * @param key    缓存键
     * @param loader 值加载器
     * @return 缓存值
     */
    V get(K key, Function<K, V> loader);

    /**
     * 将值放入缓存
     * 
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(K key, V value);

    /**
     * 将值放入缓存，并设置过期时间
     * 
     * @param key        缓存键
     * @param value      缓存值
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    void put(K key, V value, long expireTime, TimeUnit timeUnit);

    /**
     * 从缓存中移除指定键的值
     * 
     * @param key 缓存键
     * @return 如果值存在并被移除则返回true，否则返回false
     */
    boolean remove(K key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 检查缓存中是否包含指定的键
     * 
     * @param key 缓存键
     * @return 如果缓存中包含指定的键，则返回true，否则返回false
     */
    default boolean containsKey(K key) {
        return get(key) != null;
    }

    /**
     * 获取缓存名称
     * 
     * @return 缓存名称
     */
    String getName();
}