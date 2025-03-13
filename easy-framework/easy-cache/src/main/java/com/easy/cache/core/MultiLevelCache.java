package com.easy.cache.core;

import com.easy.cache.sync.CacheEvent;
import com.easy.cache.sync.CacheSyncManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存实现，支持组合多个缓存实例
 */
public class MultiLevelCache<K, V> extends AbstractCache<K, V> {

    private final List<Cache<K, V>> caches;
    private final boolean writeThrough;
    private final boolean asyncWrite;
    private final boolean syncLocal; // 是否同步本地缓存

    /**
     * 创建多级缓存
     * 
     * @param name         缓存名称
     * @param caches       缓存列表，按照优先级排序
     * @param writeThrough 是否写透
     * @param asyncWrite   是否异步写入
     * @param syncLocal    是否同步本地缓存
     */
    public MultiLevelCache(String name, List<Cache<K, V>> caches, boolean writeThrough, boolean asyncWrite,
            boolean syncLocal) {
        super(name);
        if (caches == null || caches.isEmpty()) {
            throw new IllegalArgumentException("Caches cannot be null or empty");
        }
        this.caches = new ArrayList<>(caches);
        this.writeThrough = writeThrough;
        this.asyncWrite = asyncWrite;
        this.syncLocal = syncLocal;

        // 如果启用了本地缓存同步，则注册到缓存同步管理器
        if (syncLocal) {
            CacheSyncManager.getInstance().enableSync(name, true);
        }
    }

    /**
     * 创建多级缓存（不启用本地缓存同步）
     * 
     * @param name         缓存名称
     * @param caches       缓存列表，按照优先级排序
     * @param writeThrough 是否写透
     * @param asyncWrite   是否异步写入
     */
    public MultiLevelCache(String name, List<Cache<K, V>> caches, boolean writeThrough, boolean asyncWrite) {
        this(name, caches, writeThrough, asyncWrite, false);
    }

    @Override
    public V get(K key) {
        checkKey(key);
        V value = null;
        int foundIndex = -1;

        // 按优先级查找缓存
        for (int i = 0; i < caches.size(); i++) {
            value = caches.get(i).get(key);
            if (value != null) {
                foundIndex = i;
                break;
            }
        }

        // 如果在低优先级缓存中找到值，则回填到高优先级缓存
        if (foundIndex > 0 && value != null) {
            backfill(key, value, foundIndex);
        }

        return value;
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        checkKey(key);

        if (writeThrough) {
            // 写透模式：将值写入所有缓存
            if (asyncWrite) {
                // 异步写入
                CompletableFuture.runAsync(() -> {
                    for (Cache<K, V> cache : caches) {
                        cache.put(key, value, expireTime, timeUnit);
                    }
                    // 发布缓存更新事件
                    if (syncLocal) {
                        publishPutEvent(key, value);
                    }
                });
            } else {
                // 同步写入
                for (Cache<K, V> cache : caches) {
                    cache.put(key, value, expireTime, timeUnit);
                }
                // 发布缓存更新事件
                if (syncLocal) {
                    publishPutEvent(key, value);
                }
            }
        } else {
            // 非写透模式：只写入最高优先级缓存
            if (caches.size() > 0) {
                caches.get(0).put(key, value, expireTime, timeUnit);
                // 发布缓存更新事件
                if (syncLocal) {
                    publishPutEvent(key, value);
                }
            }
        }
    }

    @Override
    public boolean remove(K key) {
        checkKey(key);
        boolean removed = false;

        if (writeThrough) {
            // 写透模式：从所有缓存中移除
            if (asyncWrite) {
                // 异步移除
                CompletableFuture.runAsync(() -> {
                    for (Cache<K, V> cache : caches) {
                        cache.remove(key);
                    }
                    // 发布缓存移除事件
                    if (syncLocal) {
                        publishRemoveEvent(key);
                    }
                });
                removed = true;
            } else {
                // 同步移除
                for (Cache<K, V> cache : caches) {
                    boolean result = cache.remove(key);
                    removed = removed || result;
                }
                // 发布缓存移除事件
                if (syncLocal) {
                    publishRemoveEvent(key);
                }
            }
        } else {
            // 非写透模式：只从最高优先级缓存中移除
            if (caches.size() > 0) {
                removed = caches.get(0).remove(key);
                // 发布缓存移除事件
                if (syncLocal) {
                    publishRemoveEvent(key);
                }
            }
        }

        return removed;
    }

    @Override
    public void clear() {
        if (writeThrough) {
            // 写透模式：清空所有缓存
            if (asyncWrite) {
                // 异步清空
                CompletableFuture.runAsync(() -> {
                    for (Cache<K, V> cache : caches) {
                        cache.clear();
                    }
                    // 发布缓存清空事件
                    if (syncLocal) {
                        publishClearEvent();
                    }
                });
            } else {
                // 同步清空
                for (Cache<K, V> cache : caches) {
                    cache.clear();
                }
                // 发布缓存清空事件
                if (syncLocal) {
                    publishClearEvent();
                }
            }
        } else {
            // 非写透模式：只清空最高优先级缓存
            if (caches.size() > 0) {
                caches.get(0).clear();
                // 发布缓存清空事件
                if (syncLocal) {
                    publishClearEvent();
                }
            }
        }
    }

    /**
     * 将值回填到高优先级缓存
     * 
     * @param key        缓存键
     * @param value      缓存值
     * @param foundIndex 找到值的缓存索引
     */
    private void backfill(K key, V value, int foundIndex) {
        // 将值回填到所有高优先级的缓存中
        for (int i = 0; i < foundIndex; i++) {
            final int index = i;
            if (asyncWrite) {
                // 异步回填
                CompletableFuture.runAsync(() -> caches.get(index).put(key, value));
            } else {
                // 同步回填
                caches.get(i).put(key, value);
            }
        }
    }

    /**
     * 发布缓存添加或更新事件
     * 
     * @param key   缓存键
     * @param value 缓存值
     */
    private void publishPutEvent(K key, V value) {
        CacheEvent event = CacheEvent.createPutEvent(name, key, value);
        CacheSyncManager.getInstance().publishEvent(event);
    }

    /**
     * 发布缓存移除事件
     * 
     * @param key 缓存键
     */
    private void publishRemoveEvent(K key) {
        CacheEvent event = CacheEvent.createRemoveEvent(name, key);
        CacheSyncManager.getInstance().publishEvent(event);
    }

    /**
     * 发布缓存清空事件
     */
    private void publishClearEvent() {
        CacheEvent event = CacheEvent.createClearEvent(name);
        CacheSyncManager.getInstance().publishEvent(event);
    }

    /**
     * 获取缓存级别数量
     * 
     * @return 缓存级别数量
     */
    public int getLevelCount() {
        return caches.size();
    }

    /**
     * 获取指定级别的缓存
     * 
     * @param level 缓存级别（从0开始）
     * @return 缓存实例
     */
    public Cache<K, V> getLevel(int level) {
        if (level < 0 || level >= caches.size()) {
            throw new IllegalArgumentException("Invalid cache level: " + level);
        }
        return caches.get(level);
    }

    /**
     * 是否启用本地缓存同步
     * 
     * @return 是否启用本地缓存同步
     */
    public boolean isSyncLocal() {
        return syncLocal;
    }
}