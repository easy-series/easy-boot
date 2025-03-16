package com.easy.lock.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 锁监控类
 */
@Slf4j
public class LockMonitor {

    /**
     * 锁成功计数器
     */
    @Getter
    private final AtomicLong successCount = new AtomicLong(0);

    /**
     * 锁失败计数器
     */
    @Getter
    private final AtomicLong failCount = new AtomicLong(0);

    /**
     * 释放锁成功计数器
     */
    @Getter
    private final AtomicLong releaseSuccessCount = new AtomicLong(0);

    /**
     * 释放锁失败计数器
     */
    @Getter
    private final AtomicLong releaseFailCount = new AtomicLong(0);

    /**
     * 锁持有时间统计（毫秒）
     */
    @Getter
    private final AtomicLong totalLockTime = new AtomicLong(0);

    /**
     * 总锁定次数
     */
    @Getter
    private final AtomicLong totalLockCount = new AtomicLong(0);

    /**
     * 锁定时间最长的记录
     */
    @Getter
    private volatile LockRecord maxTimeRecord;

    /**
     * 各资源锁定失败次数统计
     */
    private final Map<String, AtomicLong> resourceFailCountMap = new ConcurrentHashMap<>();

    /**
     * 各资源锁定成功次数统计
     */
    private final Map<String, AtomicLong> resourceSuccessCountMap = new ConcurrentHashMap<>();

    /**
     * 记录获取锁成功
     *
     * @param key 锁定资源
     */
    public void recordSuccess(String key) {
        successCount.incrementAndGet();
        totalLockCount.incrementAndGet();
        resourceSuccessCountMap.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录获取锁失败
     *
     * @param key 锁定资源
     */
    public void recordFail(String key) {
        failCount.incrementAndGet();
        resourceFailCountMap.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录释放锁成功
     */
    public void recordReleaseSuccess() {
        releaseSuccessCount.incrementAndGet();
    }

    /**
     * 记录释放锁失败
     */
    public void recordReleaseFail() {
        releaseFailCount.incrementAndGet();
    }

    /**
     * 记录锁定时间
     *
     * @param key      锁定资源
     * @param lockTime 锁定时间（毫秒）
     */
    public void recordLockTime(String key, long lockTime) {
        totalLockTime.addAndGet(lockTime);

        // 更新最大锁定时间记录
        if (maxTimeRecord == null || lockTime > maxTimeRecord.getLockTime()) {
            maxTimeRecord = new LockRecord(key, lockTime, System.currentTimeMillis());
        }
    }

    /**
     * 获取平均锁定时间（毫秒）
     *
     * @return 平均锁定时间
     */
    public double getAverageLockTime() {
        long count = totalLockCount.get();
        if (count == 0) {
            return 0;
        }
        return totalLockTime.get() / (double) count;
    }

    /**
     * 获取锁失败比例
     *
     * @return 失败比例
     */
    public double getFailRate() {
        long total = successCount.get() + failCount.get();
        if (total == 0) {
            return 0;
        }
        return failCount.get() / (double) total;
    }

    /**
     * 获取资源锁定失败次数
     *
     * @param key 资源键
     * @return 失败次数
     */
    public long getResourceFailCount(String key) {
        AtomicLong count = resourceFailCountMap.get(key);
        return count == null ? 0 : count.get();
    }

    /**
     * 获取资源锁定成功次数
     *
     * @param key 资源键
     * @return 成功次数
     */
    public long getResourceSuccessCount(String key) {
        AtomicLong count = resourceSuccessCountMap.get(key);
        return count == null ? 0 : count.get();
    }

    /**
     * 获取资源锁定争用比例
     *
     * @param key 资源键
     * @return 争用比例
     */
    public double getResourceContentionRate(String key) {
        long success = getResourceSuccessCount(key);
        long fail = getResourceFailCount(key);
        long total = success + fail;

        if (total == 0) {
            return 0;
        }

        return fail / (double) total;
    }

    /**
     * 获取所有资源的锁定情况
     *
     * @return 资源锁定情况映射
     */
    public Map<String, ResourceStats> getResourceStats() {
        Map<String, ResourceStats> result = new ConcurrentHashMap<>();

        // 合并两个Map的键集合
        for (String key : resourceSuccessCountMap.keySet()) {
            result.put(key, createResourceStats(key));
        }

        for (String key : resourceFailCountMap.keySet()) {
            if (!result.containsKey(key)) {
                result.put(key, createResourceStats(key));
            }
        }

        return result;
    }

    /**
     * 创建资源统计信息
     *
     * @param key 资源键
     * @return 资源统计信息
     */
    private ResourceStats createResourceStats(String key) {
        long success = getResourceSuccessCount(key);
        long fail = getResourceFailCount(key);

        ResourceStats stats = new ResourceStats();
        stats.setKey(key);
        stats.setSuccessCount(success);
        stats.setFailCount(fail);
        stats.setContentionRate(getResourceContentionRate(key));

        return stats;
    }

    /**
     * 重置监控数据
     */
    public void reset() {
        successCount.set(0);
        failCount.set(0);
        releaseSuccessCount.set(0);
        releaseFailCount.set(0);
        totalLockTime.set(0);
        totalLockCount.set(0);
        maxTimeRecord = null;
        resourceFailCountMap.clear();
        resourceSuccessCountMap.clear();
    }

    /**
     * 锁记录类
     */
    @Getter
    public static class LockRecord {
        /**
         * 锁定资源
         */
        private final String key;

        /**
         * 锁定时间（毫秒）
         */
        private final long lockTime;

        /**
         * 记录时间
         */
        private final long recordTime;

        public LockRecord(String key, long lockTime, long recordTime) {
            this.key = key;
            this.lockTime = lockTime;
            this.recordTime = recordTime;
        }
    }

    /**
     * 资源统计信息类
     */
    @Getter
    public static class ResourceStats {
        /**
         * 资源键
         */
        private String key;

        /**
         * 成功次数
         */
        private long successCount;

        /**
         * 失败次数
         */
        private long failCount;

        /**
         * 争用比例
         */
        private double contentionRate;

        public void setKey(String key) {
            this.key = key;
        }

        public void setSuccessCount(long successCount) {
            this.successCount = successCount;
        }

        public void setFailCount(long failCount) {
            this.failCount = failCount;
        }

        public void setContentionRate(double contentionRate) {
            this.contentionRate = contentionRate;
        }
    }
}