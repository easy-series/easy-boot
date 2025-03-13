package com.easy.cache.core;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 可自动刷新的缓存装饰器
 */
public class RefreshableCache<K, V> extends AbstractCache<K, V> {

    private final Cache<K, V> delegate;
    private final Map<K, RefreshTask> refreshTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final long refreshInterval;
    private final TimeUnit refreshTimeUnit;
    private final Map<K, Function<K, V>> loaders = new ConcurrentHashMap<>();
    private final Map<K, Long> lastAccessTimeMap = new ConcurrentHashMap<>();
    private long stopRefreshAfterLastAccess = 0; // 最后一次访问后停止刷新的时间（毫秒），0表示不停止

    /**
     * 创建可自动刷新的缓存
     * 
     * @param delegate        被装饰的缓存
     * @param refreshInterval 刷新间隔
     * @param refreshTimeUnit 刷新间隔时间单位
     * @param threadPoolSize  线程池大小
     */
    public RefreshableCache(Cache<K, V> delegate, long refreshInterval, TimeUnit refreshTimeUnit, int threadPoolSize) {
        super(delegate.getName() + ":refreshable");
        this.delegate = delegate;
        this.refreshInterval = refreshInterval;
        this.refreshTimeUnit = refreshTimeUnit;
        this.scheduler = new ScheduledThreadPoolExecutor(threadPoolSize, r -> {
            Thread thread = new Thread(r, "RefreshableCache-" + delegate.getName());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public V get(K key) {
        return delegate.get(key);
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        V value = delegate.get(key);
        if (value == null && loader != null) {
            value = loader.apply(key);
            if (value != null) {
                put(key, value);
                // 注册加载器，用于自动刷新
                registerLoader(key, loader);
            }
        }
        return value;
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        delegate.put(key, value, expireTime, timeUnit);
    }

    @Override
    public boolean remove(K key) {
        cancelRefreshTask(key);
        return delegate.remove(key);
    }

    @Override
    public void clear() {
        cancelAllRefreshTasks();
        delegate.clear();
    }

    /**
     * 注册加载器，用于自动刷新
     */
    public void registerLoader(K key, Function<K, V> loader) {
        if (key == null || loader == null) {
            return;
        }

        loaders.put(key, loader);
        scheduleRefresh(key);
    }

    /**
     * 取消指定键的刷新任务
     */
    public void cancelRefreshTask(K key) {
        RefreshTask task = refreshTasks.remove(key);
        if (task != null) {
            task.cancel();
        }
        loaders.remove(key);
    }

    /**
     * 取消所有刷新任务
     */
    public void cancelAllRefreshTasks() {
        for (RefreshTask task : refreshTasks.values()) {
            task.cancel();
        }
        refreshTasks.clear();
        loaders.clear();
    }

    /**
     * 安排刷新任务
     */
    private void scheduleRefresh(K key) {
        Function<K, V> loader = loaders.get(key);
        if (loader == null) {
            return;
        }

        // 取消已有的刷新任务
        cancelRefreshTask(key);

        // 创建新的刷新任务
        RefreshTask task = new RefreshTask(key, loader);
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                task, refreshInterval, refreshInterval, refreshTimeUnit);
        task.setFuture(future);
        refreshTasks.put(key, task);
    }

    /**
     * 刷新任务
     */
    private class RefreshTask implements Runnable {
        private final K key;
        private final Function<K, V> loader;
        private ScheduledFuture<?> future;

        public RefreshTask(K key, Function<K, V> loader) {
            this.key = key;
            this.loader = loader;
        }

        public void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        public void cancel() {
            if (future != null) {
                future.cancel(false);
            }
        }

        @Override
        public void run() {
            try {
                // 检查键是否存在
                if (delegate.get(key) == null) {
                    // 如果键不存在，取消刷新任务
                    cancelRefreshTask(key);
                    return;
                }

                // 加载新值
                V newValue = loader.apply(key);
                if (newValue != null) {
                    // 更新缓存
                    delegate.put(key, newValue);
                }
            } catch (Exception e) {
                // 忽略异常，继续下一次刷新
                System.err.println("Error refreshing cache key: " + key + ", " + e.getMessage());
            }
        }
    }

    /**
     * 设置最后一次访问后停止刷新的时间
     * 
     * @param stopRefreshAfterLastAccess 最后一次访问后停止刷新的时间，单位与创建缓存时指定的时间单位相同
     */
    public void setStopRefreshAfterLastAccess(long stopRefreshAfterLastAccess) {
        this.stopRefreshAfterLastAccess = stopRefreshAfterLastAccess > 0
                ? TimeUnit.MILLISECONDS.convert(stopRefreshAfterLastAccess, refreshTimeUnit)
                : 0;
    }
}