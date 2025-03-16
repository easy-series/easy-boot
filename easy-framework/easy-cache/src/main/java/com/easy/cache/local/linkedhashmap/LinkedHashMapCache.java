package com.easy.cache.local.linkedhashmap;

import com.easy.cache.config.CacheConfig;
import com.easy.cache.core.AbstractCache;
import com.easy.cache.support.NullValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于LinkedHashMap的简单本地缓存实现
 * 
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class LinkedHashMapCache<K, V> extends AbstractCache<K, V> {
    
    /**
     * 缓存数据
     */
    private final Map<K, CacheEntry<V>> cache;
    
    /**
     * 读写锁
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    
    /**
     * 构造函数
     * 
     * @param name 缓存名称
     * @param config 缓存配置
     */
    public LinkedHashMapCache(String name, CacheConfig<K, V> config) {
        super(name, config);
        
        // 创建访问顺序的LinkedHashMap，并限制最大容量
        final int maxSize = config.getLocalLimit();
        
        cache = Collections.synchronizedMap(new LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > maxSize;
            }
        });
    }
    
    @Override
    public V get(K key) {
        readLock.lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            
            if (entry == null) {
                stats.recordHit(false);
                return null;
            }
            
            // 如果已过期，则移除
            if (entry.isExpired()) {
                writeLock.lock();
                try {
                    cache.remove(key);
                } finally {
                    writeLock.unlock();
                }
                stats.recordHit(false);
                return null;
            }
            
            stats.recordHit(true);
            return processValue(entry.getValue());
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public void put(K key, V value) {
        writeLock.lock();
        try {
            long expireTime = 0;
            if (config.getExpireAfterWrite() > 0) {
                expireTime = System.currentTimeMillis() + config.getTimeUnit().toMillis(config.getExpireAfterWrite());
            }
            
            Object cacheValue = value;
            if (value == null && config.isCacheNullValue()) {
                cacheValue = NullValue.INSTANCE;
            }
            
            cache.put(key, new CacheEntry<>(cacheValue, expireTime));
        } finally {
            writeLock.unlock();
        }
    }
    
    @Override
    public void put(K key, V value, long expire, TimeUnit timeUnit) {
        writeLock.lock();
        try {
            long expireTime = 0;
            if (expire > 0) {
                expireTime = System.currentTimeMillis() + timeUnit.toMillis(expire);
            }
            
            Object cacheValue = value;
            if (value == null && config.isCacheNullValue()) {
                cacheValue = NullValue.INSTANCE;
            }
            
            cache.put(key, new CacheEntry<>(cacheValue, expireTime));
        } finally {
            writeLock.unlock();
        }
    }
    
    @Override
    public boolean remove(K key) {
        writeLock.lock();
        try {
            return cache.remove(key) != null;
        } finally {
            writeLock.unlock();
        }
    }
    
    @Override
    public boolean containsKey(K key) {
        readLock.lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null) {
                return false;
            }
            
            // 如果已过期，则移除
            if (entry.isExpired()) {
                writeLock.lock();
                try {
                    cache.remove(key);
                } finally {
                    writeLock.unlock();
                }
                return false;
            }
            
            return true;
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public void clear() {
        writeLock.lock();
        try {
            cache.clear();
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存大小
     */
    public int size() {
        readLock.lock();
        try {
            return cache.size();
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * 清理过期的缓存项
     */
    public void cleanUp() {
        writeLock.lock();
        try {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * 缓存条目，包含值和过期时间
     * 
     * @param <V> 值类型
     */
    private static class CacheEntry<V> {
        
        /**
         * 缓存值
         */
        private final Object value;
        
        /**
         * 过期时间（毫秒时间戳）
         */
        private final long expireTime;
        
        /**
         * 构造函数
         * 
         * @param value 缓存值
         * @param expireTime 过期时间
         */
        CacheEntry(Object value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
        
        /**
         * 获取缓存值
         * 
         * @return 缓存值
         */
        @SuppressWarnings("unchecked")
        public V getValue() {
            return (V) value;
        }
        
        /**
         * 检查是否已过期
         * 
         * @return 是否已过期
         */
        public boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }
    }
} 