package com.easy.cache.core;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存实现，结合本地缓存和远程缓存
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class MultiLevelCache<K, V> extends AbstractCache<K, V> {

    /**
     * 本地缓存
     */
    private final Cache<K, V> localCache;

    /**
     * 远程缓存
     */
    private final Cache<K, V> remoteCache;

    /**
     * 是否使用写穿透模式
     * 写穿透模式：写入本地缓存的同时写入远程缓存
     */
    private final boolean writeThrough;

    /**
     * 是否异步写入远程缓存
     */
    private final boolean asyncWrite;

    /**
     * 构造函数
     *
     * @param name 缓存名称
     * @param localCache 本地缓存
     * @param remoteCache 远程缓存
     * @param writeThrough 是否使用写穿透模式
     * @param asyncWrite 是否异步写入远程缓存
     */
    public MultiLevelCache(String name, Cache<K, V> localCache, Cache<K, V> remoteCache,
                          boolean writeThrough, boolean asyncWrite) {
        super(name);
        this.localCache = localCache;
        this.remoteCache = remoteCache;
        this.writeThrough = writeThrough;
        this.asyncWrite = asyncWrite;
    }

    @Override
    public V get(K key) {
        // 先从本地缓存获取
        V value = localCache.get(key);
        if (value != null) {
            return value;
        }

        // 本地缓存未命中，从远程缓存获取
        value = remoteCache.get(key);
        if (value != null) {
            // 将远程缓存的值加载到本地缓存
            localCache.put(key, value);
        }
        return value;
    }

    @Override
    public void put(K key, V value, long expire, TimeUnit timeUnit) {
        // 写入本地缓存
        localCache.put(key, value, expire, timeUnit);

        // 如果启用写穿透，同时写入远程缓存
        if (writeThrough) {
            if (asyncWrite) {
                // 异步写入远程缓存
                new Thread(() -> remoteCache.put(key, value, expire, timeUnit)).start();
            } else {
                // 同步写入远程缓存
                remoteCache.put(key, value, expire, timeUnit);
            }
        }
    }

    @Override
    public boolean remove(K key) {
        boolean localRemoved = localCache.remove(key);
        boolean remoteRemoved = remoteCache.remove(key);
        return localRemoved || remoteRemoved;
    }

    @Override
    public void clear() {
        localCache.clear();
        remoteCache.clear();
    }

    @Override
    public long size() {
        // 返回远程缓存大小，更准确
        return remoteCache.size();
    }
} 