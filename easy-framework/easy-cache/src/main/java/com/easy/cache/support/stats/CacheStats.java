package com.easy.cache.support.stats;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 缓存统计信息
 */
@Getter
public class CacheStats {
    
    /**
     * 缓存名称
     */
    private final String cacheName;
    
    /**
     * 请求总数
     */
    private final LongAdder requestCount = new LongAdder();
    
    /**
     * 命中总数
     */
    private final LongAdder hitCount = new LongAdder();
    
    /**
     * 未命中总数
     */
    private final LongAdder missCount = new LongAdder();
    
    /**
     * 加载成功次数
     */
    private final LongAdder loadSuccessCount = new LongAdder();
    
    /**
     * 加载失败次数
     */
    private final LongAdder loadFailureCount = new LongAdder();
    
    /**
     * 总响应时间（纳秒）
     */
    private final LongAdder totalLoadTime = new LongAdder();
    
    /**
     * 最大响应时间（纳秒）
     */
    private final AtomicLong maxLoadTime = new AtomicLong();
    
    /**
     * 最后一次统计时间
     */
    private volatile long lastResetTime = System.currentTimeMillis();
    
    /**
     * 构造方法
     *
     * @param cacheName 缓存名称
     */
    public CacheStats(String cacheName) {
        this.cacheName = cacheName;
    }
    
    /**
     * 记录请求
     */
    public void recordRequest() {
        requestCount.increment();
    }
    
    /**
     * 记录命中
     */
    public void recordHit() {
        hitCount.increment();
    }
    
    /**
     * 记录未命中
     */
    public void recordMiss() {
        missCount.increment();
    }
    
    /**
     * 记录加载成功
     */
    public void recordLoadSuccess() {
        loadSuccessCount.increment();
    }
    
    /**
     * 记录加载失败
     */
    public void recordLoadFailure() {
        loadFailureCount.increment();
    }
    
    /**
     * 记录加载时间
     *
     * @param loadTimeNanos 加载时间（纳秒）
     */
    public void recordLoadTime(long loadTimeNanos) {
        totalLoadTime.add(loadTimeNanos);
        
        // 更新最大加载时间
        while (true) {
            long current = maxLoadTime.get();
            if (loadTimeNanos <= current || maxLoadTime.compareAndSet(current, loadTimeNanos)) {
                break;
            }
        }
    }
    
    /**
     * 获取命中率
     *
     * @return 命中率
     */
    public double getHitRate() {
        long totalCount = requestCount.sum();
        return totalCount == 0 ? 0.0 : (double) hitCount.sum() / totalCount;
    }
    
    /**
     * 获取平均加载时间（毫秒）
     *
     * @return 平均加载时间
     */
    public double getAverageLoadTimeMillis() {
        long loadCount = loadSuccessCount.sum() + loadFailureCount.sum();
        return loadCount == 0 ? 0.0 : (double) totalLoadTime.sum() / loadCount / 1_000_000.0;
    }
    
    /**
     * 获取最大加载时间（毫秒）
     *
     * @return 最大加载时间
     */
    public double getMaxLoadTimeMillis() {
        return maxLoadTime.get() / 1_000_000.0;
    }
    
    /**
     * 重置统计数据
     */
    public void reset() {
        requestCount.reset();
        hitCount.reset();
        missCount.reset();
        loadSuccessCount.reset();
        loadFailureCount.reset();
        totalLoadTime.reset();
        maxLoadTime.set(0);
        lastResetTime = System.currentTimeMillis();
    }
    
    /**
     * 获取统计报告
     *
     * @return 统计报告
     */
    public String getReport() {
        return String.format(
            "Cache Stats - %s: hits=%d, misses=%d, hit_rate=%.2f%%, load_success=%d, load_failure=%d, avg_load_time=%.2fms, max_load_time=%.2fms",
            cacheName,
            hitCount.sum(),
            missCount.sum(),
            getHitRate() * 100.0,
            loadSuccessCount.sum(),
            loadFailureCount.sum(),
            getAverageLoadTimeMillis(),
            getMaxLoadTimeMillis()
        );
    }
} 