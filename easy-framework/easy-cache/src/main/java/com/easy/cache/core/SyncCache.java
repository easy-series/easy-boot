package com.easy.cache.core;

import com.easy.cache.event.CacheEvent;
import com.easy.cache.event.CacheSyncManager;

import java.util.concurrent.TimeUnit;

/**
 * 支持缓存同步的缓存实现
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class SyncCache<K, V> extends AbstractCache<K, V> {

    /**
     * 被装饰的缓存
     */
    private final Cache<K, V> delegate;

    /**
     * 缓存同步管理器
     */
    private final CacheSyncManager syncManager;

    /**
     * 构造函数
     *
     * @param delegate 被装饰的缓存
     * @param syncManager 缓存同步管理器
     */
    public SyncCache(Cache<K, V> delegate, CacheSyncManager syncManager) {
        super(delegate.getName());
        this.delegate = delegate;
        this.syncManager = syncManager;
    }

    @Override
    public V get(K key) {
        return delegate.get(key);
    }

    @Override
    public void put(K key, V value, long expire, TimeUnit timeUnit) {
        delegate.put(key, value, expire, timeUnit);
        
        // 发布PUT事件
        CacheEvent event = CacheEvent.createPutEvent(getName(), key.toString(), value);
        syncManager.publishEvent(event);
    }

    @Override
    public boolean remove(K key) {
        boolean removed = delegate.remove(key);
        
        if (removed) {
            // 发布REMOVE事件
            CacheEvent event = CacheEvent.createRemoveEvent(getName(), key.toString());
            syncManager.publishEvent(event);
        }
        
        return removed;
    }

    @Override
    public void clear() {
        delegate.clear();
        
        // 发布CLEAR事件
        CacheEvent event = CacheEvent.createClearEvent(getName());
        syncManager.publishEvent(event);
    }

    @Override
    public boolean contains(K key) {
        return delegate.contains(key);
    }

    @Override
    public long size() {
        return delegate.size();
    }
} 