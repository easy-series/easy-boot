package com.easy.cache.monitor;

import java.util.concurrent.atomic.AtomicLong;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 缓存统计信息
 */
@Data
@Accessors(chain = true)
public class CacheStats {
    /**
     * 缓存命中次数
     */
    private final AtomicLong hits = new AtomicLong();

    /**
     * 缓存未命中次数
     */
    private final AtomicLong misses = new AtomicLong();

    /**
     * 记录命中
     */
    public void recordHit() {
        hits.incrementAndGet();
    }

    /**
     * 记录未命中
     */
    public void recordMiss() {
        misses.incrementAndGet();
    }

    /**
     * 获取命中率
     *
     * @return 命中率
     */
    public double getHitRate() {
        long hitsCount = hits.get();
        long missesCount = misses.get();
        long total = hitsCount + missesCount;
        return total == 0 ? 0 : (double) hitsCount / total;
    }

    /**
     * 获取总请求次数
     *
     * @return 总请求次数
     */
    public long getRequestCount() {
        return hits.get() + misses.get();
    }
}