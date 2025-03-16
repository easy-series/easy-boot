package com.easy.cache.core.multi;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.easy.cache.core.AbstractCache;
import com.easy.cache.core.embedded.LocalCache;
import com.easy.cache.support.convertor.KeyConvertor;

import lombok.extern.slf4j.Slf4j;

/**
 * 多级缓存实现，结合本地缓存和远程缓存
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class MultiLevelCache<K, V> extends AbstractCache<K, V> {

    /**
     * 本地缓存
     */
    private final LocalCache<K, V> localCache;

    /**
     * 远程缓存
     */
    private final AbstractCache<K, V> remoteCache;

    /**
     * 是否使用写透策略（先写远程，再写本地）
     */
    private final boolean writeThrough;

    /**
     * 是否异步写入远程缓存
     */
    private final boolean asyncWrite;

    /**
     * 构造方法
     *
     * @param name               缓存名称
     * @param keyConvertor       键转换器
     * @param localCache         本地缓存
     * @param remoteCache        远程缓存
     * @param defaultExpiration  默认过期时间
     * @param loader             缓存加载器
     * @param cacheNullValues    是否缓存null值
     * @param penetrationProtect 是否启用缓存穿透保护
     * @param writeThrough       是否使用写透策略
     * @param asyncWrite         是否异步写入远程缓存
     */
    public MultiLevelCache(String name, KeyConvertor keyConvertor, LocalCache<K, V> localCache,
            AbstractCache<K, V> remoteCache, Duration defaultExpiration,
            Function<K, V> loader, boolean cacheNullValues, boolean penetrationProtect,
            boolean writeThrough, boolean asyncWrite) {
        super(name, keyConvertor, defaultExpiration, loader, cacheNullValues, penetrationProtect);
        this.localCache = localCache;
        this.remoteCache = remoteCache;
        this.writeThrough = writeThrough;
        this.asyncWrite = asyncWrite;
    }

    @Override
    public V get(K key) {
        stats.recordRequest();

        // 先从本地缓存获取
        V value = localCache.get(key);
        if (value != null) {
            stats.recordHit();
            return value;
        }

        // 从远程缓存获取
        value = remoteCache.get(key);

        // 如果远程缓存命中，则放入本地缓存
        if (value != null) {
            localCache.put(key, value);
            return value;
        }

        // 如果有加载器，使用加载器加载
        if (loader != null) {
            if (penetrationProtect) {
                return loadWithProtection(key);
            } else {
                value = loadValue(key);
                if (value != null || cacheNullValues) {
                    put(key, value);
                }
                return value;
            }
        }

        return null;
    }

    /**
     * 使用缓存穿透保护机制加载值
     *
     * @param key 键
     * @return 加载的值
     */
    private V loadWithProtection(K key) {
        // 使用远程缓存的锁实现分布式锁
        if (remoteCache.tryLock(key, Duration.ofSeconds(5))) {
            try {
                // 再次检查缓存，防止其他进程已经加载
                V value = remoteCache.get(key);
                if (value != null) {
                    // 如果远程已有值，放入本地缓存
                    localCache.put(key, value);
                    return value;
                }

                // 加载值
                value = loadValue(key);
                if (value != null || cacheNullValues) {
                    put(key, value);
                }
                return value;
            } finally {
                unlock(key);
            }
        } else {
            // 未获取到锁，等待一段时间后再次尝试获取缓存
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 再次尝试获取
            V value = remoteCache.get(key);
            if (value != null) {
                localCache.put(key, value);
                return value;
            }
            return null;
        }
    }

    /**
     * 加载值
     *
     * @param key 键
     * @return 加载的值
     */
    private V loadValue(K key) {
        stats.recordMiss();

        if (loader == null) {
            return null;
        }

        long startTime = System.nanoTime();
        try {
            V value = loader.apply(key);
            stats.recordLoadSuccess();
            return value;
        } catch (Exception e) {
            stats.recordLoadFailure();
            log.error("加载缓存值失败: key={}", key, e);
            throw e;
        } finally {
            long loadTime = System.nanoTime() - startTime;
            stats.recordLoadTime(loadTime);
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, defaultExpiration);
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        if (value != null || cacheNullValues) {
            if (writeThrough) {
                // 写透策略：先写远程，再写本地
                if (asyncWrite) {
                    // 异步写入远程缓存
                    CompletableFuture.runAsync(() -> remoteCache.put(key, value, ttl), executor);
                } else {
                    // 同步写入远程缓存
                    remoteCache.put(key, value, ttl);
                }
                // 写入本地缓存
                localCache.put(key, value, ttl);
            } else {
                // 先写本地，再写远程
                localCache.put(key, value, ttl);
                if (asyncWrite) {
                    // 异步写入远程缓存
                    CompletableFuture.runAsync(() -> remoteCache.put(key, value, ttl), executor);
                } else {
                    // 同步写入远程缓存
                    remoteCache.put(key, value, ttl);
                }
            }
        }
    }

    @Override
    public boolean remove(K key) {
        boolean result = remoteCache.remove(key);
        localCache.remove(key);
        return result;
    }

    @Override
    public void clear() {
        remoteCache.clear();
        localCache.clear();
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        stats.recordRequest();

        // 先从本地缓存获取
        V value = localCache.get(key);
        if (value != null) {
            stats.recordHit();
            return value;
        }

        // 从远程缓存获取
        value = remoteCache.get(key);
        if (value != null) {
            localCache.put(key, value);
            return value;
        }

        // 使用提供的加载器加载值
        stats.recordMiss();
        long startTime = System.nanoTime();
        try {
            value = loader.apply(key);
            stats.recordLoadSuccess();

            // 缓存值
            if (value != null || cacheNullValues) {
                put(key, value, ttl);
            }

            return value;
        } catch (Exception e) {
            stats.recordLoadFailure();
            log.error("计算缓存值失败: key={}", key, e);
            throw e;
        } finally {
            long loadTime = System.nanoTime() - startTime;
            stats.recordLoadTime(loadTime);
        }
    }

    @Override
    public boolean tryLock(K key, Duration ttl) {
        // 使用远程缓存实现分布式锁
        return remoteCache.tryLock(key, ttl);
    }

    @Override
    public void unlock(K key) {
        // 使用远程缓存解锁
        try {
            remoteCache.unlock(key);
        } catch (Exception e) {
            log.warn("解锁失败: key={}", key, e);
        }
    }
}