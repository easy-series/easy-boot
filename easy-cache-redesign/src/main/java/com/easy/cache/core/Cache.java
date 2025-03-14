package com.easy.cache.core;

import java.util.Collection;
import java.util.Map;
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
     * 从缓存中获取数据
     *
     * @param key 缓存键
     * @return 缓存值，如果不存在则返回null
     */
    V get(K key);

    /**
     * 从缓存中获取数据，如果不存在则调用loader加载并缓存
     *
     * @param key    缓存键
     * @param loader 数据加载器
     * @return 缓存值
     */
    V get(K key, Function<K, V> loader);

    /**
     * 将数据放入缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(K key, V value);

    /**
     * 将数据放入缓存并设置过期时间
     *
     * @param key        缓存键
     * @param value      缓存值
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    void put(K key, V value, long expireTime, TimeUnit timeUnit);

    /**
     * 从缓存中删除数据
     *
     * @param key 缓存键
     * @return 如果缓存中存在该键并删除成功则返回true，否则返回false
     */
    boolean remove(K key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 批量获取缓存数据
     *
     * @param keys 缓存键集合
     * @return 键值对映射
     */
    Map<K, V> getAll(Collection<K> keys);

    /**
     * 批量放入缓存数据
     *
     * @param map 键值对映射
     */
    void putAll(Map<K, V> map);

    /**
     * 批量放入缓存数据并设置过期时间
     *
     * @param map        键值对映射
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    void putAll(Map<K, V> map, long expireTime, TimeUnit timeUnit);

    /**
     * 批量删除缓存数据
     *
     * @param keys 缓存键集合
     * @return 如果至少有一个键被成功删除则返回true，否则返回false
     */
    boolean removeAll(Collection<K> keys);

    /**
     * 判断缓存中是否存在指定键
     *
     * @param key 缓存键
     * @return 如果缓存中存在该键则返回true，否则返回false
     */
    boolean containsKey(K key);
}