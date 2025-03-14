package com.easy.cache.core;

import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 可刷新缓存实现，支持自动刷新
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class RefreshableCache<K, V> extends AbstractCache<K, V> {

    /**
     * 被装饰的缓存
     */
    private final Cache<K, V> delegate;

    /**
     * 刷新任务调度器
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 刷新任务映射
     */
    private final ConcurrentMap<K, ScheduledFuture<?>> refreshTasks = new ConcurrentHashMap<>();

    /**
     * 值加载器
     */
    private final Function<K, V> valueLoader;

    /**
     * 是否随机延迟刷新
     */
    private final boolean randomDelay;

    /**
     * 最大随机延迟时间(秒)
     */
    private final int maxRandomDelay;

    /**
     * 构造函数
     *
     * @param delegate 被装饰的缓存
     * @param valueLoader 值加载器
     * @param randomDelay 是否随机延迟刷新
     * @param maxRandomDelay 最大随机延迟时间(秒)
     */
    public RefreshableCache(Cache<K, V> delegate, Function<K, V> valueLoader,
                           boolean randomDelay, int maxRandomDelay) {
        super(delegate.getName() + "_refreshable");
        this.delegate = delegate;
        this.valueLoader = valueLoader;
        this.randomDelay = randomDelay;
        this.maxRandomDelay = maxRandomDelay;
        this.scheduler = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread thread = new Thread(r, "cache-refresh-thread");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @Override
    public V get(K key) {
        return delegate.get(key);
    }

    @Override
    public void put(K key, V value, long expire, TimeUnit timeUnit) {
        delegate.put(key, value, expire, timeUnit);
    }

    /**
     * 放入可刷新的缓存
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param expire 过期时间
     * @param timeUnit 时间单位
     * @param refreshPeriod 刷新周期
     * @param refreshTimeUnit 刷新周期时间单位
     */
    public void putWithRefresh(K key, V value, long expire, TimeUnit timeUnit,
                              long refreshPeriod, TimeUnit refreshTimeUnit) {
        delegate.put(key, value, expire, timeUnit);
        scheduleRefresh(key, refreshPeriod, refreshTimeUnit);
    }

    /**
     * 调度刷新任务
     *
     * @param key 缓存键
     * @param refreshPeriod 刷新周期
     * @param refreshTimeUnit 刷新周期时间单位
     */
    private void scheduleRefresh(K key, long refreshPeriod, TimeUnit refreshTimeUnit) {
        // 取消已存在的刷新任务
        cancelRefreshTask(key);

        // 计算延迟时间
        long initialDelay = refreshPeriod;
        if (randomDelay) {
            // 添加随机延迟，防止缓存雪崩
            ThreadLocalRandom random = ThreadLocalRandom.current();
            initialDelay += random.nextLong(0, maxRandomDelay);
        }

        // 调度新的刷新任务
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> refreshKey(key),
                initialDelay,
                refreshPeriod,
                refreshTimeUnit);

        refreshTasks.put(key, future);
    }

    /**
     * 刷新指定键的缓存
     *
     * @param key 缓存键
     */
    private void refreshKey(K key) {
        try {
            V newValue = valueLoader.apply(key);
            if (newValue != null) {
                delegate.put(key, newValue);
            }
        } catch (Exception e) {
            // 刷新失败，记录日志，但不中断后续刷新
            System.err.println("Failed to refresh cache key: " + key + ", error: " + e.getMessage());
        }
    }

    /**
     * 取消刷新任务
     *
     * @param key 缓存键
     */
    private void cancelRefreshTask(K key) {
        ScheduledFuture<?> future = refreshTasks.remove(key);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public boolean remove(K key) {
        cancelRefreshTask(key);
        return delegate.remove(key);
    }

    @Override
    public void clear() {
        // 取消所有刷新任务
        refreshTasks.forEach((key, future) -> future.cancel(false));
        refreshTasks.clear();

        delegate.clear();
    }

    @Override
    public boolean contains(K key) {
        return delegate.contains(key);
    }

    @Override
    public long size() {
        return delegate.size();
    }

    /**
     * 关闭缓存，释放资源
     */
    public void shutdown() {
        clear();
        scheduler.shutdown();
    }
} 