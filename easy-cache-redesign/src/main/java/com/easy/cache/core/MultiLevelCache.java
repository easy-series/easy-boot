package com.easy.cache.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存实现，组合多个缓存
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public class MultiLevelCache<K, V> extends AbstractCache<K, V> {

    /**
     * 缓存层级列表
     */
    private final List<Cache<K, V>> caches;

    /**
     * 是否写透（写入时同时写入所有层级）
     */
    private final boolean writeThrough;

    /**
     * 是否异步写入下层缓存
     */
    private final boolean asyncWrite;

    /**
     * 异步写入线程池
     */
    private final ExecutorService asyncExecutor;

    /**
     * 构造函数
     *
     * @param name   缓存名称
     * @param caches 缓存层级列表
     */
    public MultiLevelCache(String name, List<Cache<K, V>> caches) {
        this(name, caches, true, false);
    }

    /**
     * 构造函数
     *
     * @param name         缓存名称
     * @param caches       缓存层级列表
     * @param writeThrough 是否写透
     * @param asyncWrite   是否异步写入
     */
    public MultiLevelCache(String name, List<Cache<K, V>> caches, boolean writeThrough, boolean asyncWrite) {
        super(name);

        if (caches == null || caches.isEmpty()) {
            throw new IllegalArgumentException("Caches不能为空");
        }

        this.caches = new ArrayList<>(caches);
        this.writeThrough = writeThrough;
        this.asyncWrite = asyncWrite;

        if (asyncWrite) {
            this.asyncExecutor = Executors.newFixedThreadPool(1, r -> {
                Thread t = new Thread(r, "MultiLevelCache-" + name + "-AsyncWriter");
                t.setDaemon(true);
                return t;
            });
        } else {
            this.asyncExecutor = null;
        }
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        V value = null;
        int foundAt = -1;

        // 从上层开始查找
        for (int i = 0; i < caches.size(); i++) {
            value = caches.get(i).get(key);
            if (value != null) {
                foundAt = i;
                break;
            }
        }

        // 如果在下层找到，回填到上层
        if (foundAt > 0) {
            int finalFoundAt = foundAt;
            V finalValue = value;

            Runnable backfillTask = () -> {
                for (int i = 0; i < finalFoundAt; i++) {
                    caches.get(i).put(key, finalValue);
                }
            };

            if (asyncWrite && asyncExecutor != null) {
                asyncExecutor.execute(backfillTask);
            } else {
                backfillTask.run();
            }
        }

        return value;
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (key == null) {
            return;
        }

        if (writeThrough) {
            // 写透模式：写入所有层级
            Runnable writeTask = () -> {
                for (int i = 1; i < caches.size(); i++) {
                    caches.get(i).put(key, value, expireTime, timeUnit);
                }
            };

            // 先写入第一层
            caches.get(0).put(key, value, expireTime, timeUnit);

            // 异步写入其他层
            if (asyncWrite && asyncExecutor != null) {
                asyncExecutor.execute(writeTask);
            } else {
                writeTask.run();
            }
        } else {
            // 只写入第一层
            caches.get(0).put(key, value, expireTime, timeUnit);
        }
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }

        boolean result = false;

        // 从所有层级删除
        for (Cache<K, V> cache : caches) {
            result |= cache.remove(key);
        }

        return result;
    }

    @Override
    public void clear() {
        // 清空所有层级
        for (Cache<K, V> cache : caches) {
            cache.clear();
        }
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<K, V> result = new HashMap<>(keys.size());
        Collection<K> remainingKeys = new ArrayList<>(keys);

        // 从上层开始查找
        for (int level = 0; level < caches.size() && !remainingKeys.isEmpty(); level++) {
            Cache<K, V> cache = caches.get(level);
            Map<K, V> levelResult = cache.getAll(remainingKeys);

            if (!levelResult.isEmpty()) {
                // 将结果合并到最终结果
                result.putAll(levelResult);

                // 更新剩余键
                remainingKeys.removeAll(levelResult.keySet());

                // 回填到上层
                if (level > 0) {
                    final int currentLevel = level;
                    Map<K, V> finalLevelResult = levelResult;

                    Runnable backfillTask = () -> {
                        for (int i = 0; i < currentLevel; i++) {
                            caches.get(i).putAll(finalLevelResult);
                        }
                    };

                    if (asyncWrite && asyncExecutor != null) {
                        asyncExecutor.execute(backfillTask);
                    } else {
                        backfillTask.run();
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void putAll(Map<K, V> map, long expireTime, TimeUnit timeUnit) {
        if (map == null || map.isEmpty()) {
            return;
        }

        if (writeThrough) {
            // 写透模式：写入所有层级
            Runnable writeTask = () -> {
                for (int i = 1; i < caches.size(); i++) {
                    caches.get(i).putAll(map, expireTime, timeUnit);
                }
            };

            // 先写入第一层
            caches.get(0).putAll(map, expireTime, timeUnit);

            // 异步写入其他层
            if (asyncWrite && asyncExecutor != null) {
                asyncExecutor.execute(writeTask);
            } else {
                writeTask.run();
            }
        } else {
            // 只写入第一层
            caches.get(0).putAll(map, expireTime, timeUnit);
        }
    }

    @Override
    public boolean removeAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return false;
        }

        boolean result = false;

        // 从所有层级删除
        for (Cache<K, V> cache : caches) {
            result |= cache.removeAll(keys);
        }

        return result;
    }

    /**
     * 关闭缓存，释放资源
     */
    public void shutdown() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
    }
}