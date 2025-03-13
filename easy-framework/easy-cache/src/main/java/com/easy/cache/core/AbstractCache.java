package com.easy.cache.core;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 抽象缓存实现，提供通用功能
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    protected final String name;

    public AbstractCache(String name) {
        this.name = name;
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
        put(key, value, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 检查键是否为null
     */
    protected void checkKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Cache key cannot be null");
        }
    }
}