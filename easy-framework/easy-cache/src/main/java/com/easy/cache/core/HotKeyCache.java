package com.easy.cache.core;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 热点数据缓存装饰器，用于防止缓存击穿
 */
public class HotKeyCache<K, V> extends AbstractCache<K, V> {
    
    private final Cache<K, V> delegate;
    private final LoadingCache<K, AtomicInteger> accessCounter;
    private final int threshold;
    private final long localExpire;
    private final TimeUnit localExpireUnit;
    
    /**
     * 创建热点数据缓存
     * 
     * @param delegate 被装饰的缓存
     * @param threshold 访问阈值
     * @param timeWindow 时间窗口
     * @param timeUnit 时间单位
     * @param localExpire 本地缓存过期时间
     * @param localExpireUnit 本地缓存过期时间单位
     */
    public HotKeyCache(Cache<K, V> delegate, int threshold, long timeWindow, TimeUnit timeUnit,
                      long localExpire, TimeUnit localExpireUnit) {
        super(delegate.getName() + ":hotkey");
        this.delegate = delegate;
        this.threshold = threshold;
        this.localExpire = localExpire;
        this.localExpireUnit = localExpireUnit;
        
        this.accessCounter = CacheBuilder.newBuilder()
            .expireAfterWrite(timeWindow, timeUnit)
            .build(new CacheLoader<K, AtomicInteger>() {
                @Override
                public AtomicInteger load(K key) {
                    return new AtomicInteger(0);
                }
            });
    }
    
    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }
        
        // 记录访问次数
        AtomicInteger counter = accessCounter.getUnchecked(key);
        int count = counter.incrementAndGet();
        
        // 从委托缓存获取值
        V value = delegate.get(key);
        
        // 如果访问次数超过阈值，且值不为null，则将其标记为热点数据
        if (count > threshold && value != null && delegate instanceof MultiLevelCache) {
            MultiLevelCache<K, V> multiLevelCache = (MultiLevelCache<K, V>) delegate;
            // 在本地缓存中保存更长时间
            multiLevelCache.putLocal(key, value, localExpire, localExpireUnit);
        }
        
        return value;
    }
    
    @Override
    public V get(K key, Function<K, V> loader) {
        if (key == null) {
            return null;
        }
        
        // 记录访问次数
        AtomicInteger counter = accessCounter.getUnchecked(key);
        int count = counter.incrementAndGet();
        
        // 从委托缓存获取值
        V value = delegate.get(key, loader);
        
        // 如果访问次数超过阈值，且值不为null，则将其标记为热点数据
        if (count > threshold && value != null && delegate instanceof MultiLevelCache) {
            MultiLevelCache<K, V> multiLevelCache = (MultiLevelCache<K, V>) delegate;
            // 在本地缓存中保存更长时间
            multiLevelCache.putLocal(key, value, localExpire, localExpireUnit);
        }
        
        return value;
    }
    
    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (key == null) {
            return;
        }
        
        // 获取当前访问次数
        AtomicInteger counter = accessCounter.getUnchecked(key);
        int count = counter.get();
        
        // 如果是热点数据，且委托缓存是多级缓存，则在本地缓存中保存更长时间
        if (count > threshold && delegate instanceof MultiLevelCache) {
            MultiLevelCache<K, V> multiLevelCache = (MultiLevelCache<K, V>) delegate;
            multiLevelCache.putLocal(key, value, localExpire, localExpireUnit);
            multiLevelCache.putRemote(key, value, expireTime, timeUnit);
        } else {
            delegate.put(key, value, expireTime, timeUnit);
        }
    }
    
    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }
        
        // 移除访问计数
        accessCounter.invalidate(key);
        
        // 从委托缓存中移除
        return delegate.remove(key);
    }
    
    @Override
    public void clear() {
        // 清空访问计数
        accessCounter.invalidateAll();
        
        // 清空委托缓存
        delegate.clear();
    }
    
    /**
     * 获取键的访问次数
     * 
     * @param key 缓存键
     * @return 访问次数
     */
    public int getAccessCount(K key) {
        AtomicInteger counter = accessCounter.getIfPresent(key);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * 判断键是否为热点数据
     * 
     * @param key 缓存键
     * @return 是否为热点数据
     */
    public boolean isHotKey(K key) {
        return getAccessCount(key) > threshold;
    }
} 