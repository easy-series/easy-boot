package com.easy.cache.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 抽象缓存实现，提供一些通用方法
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    /**
     * 缓存名称
     */
    protected final String name;

    /**
     * 构造函数
     *
     * @param name 缓存名称
     */
    protected AbstractCache(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        V value = get(key);
        if (value == null && loader != null) {
            value = loader.apply(key);
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }

    @Override
    public void put(K key, V value) {
        put(key, value, 0, TimeUnit.SECONDS);
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        Map<K, V> result = new HashMap<>(keys.size());
        for (K key : keys) {
            V value = get(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    @Override
    public void putAll(Map<K, V> map) {
        putAll(map, 0, TimeUnit.SECONDS);
    }

    @Override
    public void putAll(Map<K, V> map, long expireTime, TimeUnit timeUnit) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue(), expireTime, timeUnit);
        }
    }

    @Override
    public boolean removeAll(Collection<K> keys) {
        boolean result = false;
        for (K key : keys) {
            result |= remove(key);
        }
        return result;
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }
}