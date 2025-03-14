package com.easy.cache.core;

import java.util.concurrent.TimeUnit;

/**
 * 缓存接口的抽象实现类
 *
 * @param <K> 键类型
 * @param <V> 值类型
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
    public void put(K key, V value) {
        // 默认不过期
        put(key, value, 0, TimeUnit.SECONDS);
    }

    @Override
    public boolean contains(K key) {
        return get(key) != null;
    }
} 