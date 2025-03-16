package com.easy.id.monitor;

import com.easy.id.core.IdGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器监控类
 * 
 * 用于监控ID生成的性能和状态
 *
 * @author 芋道源码
 */
@Slf4j
public class IdGeneratorMonitor {

    /**
     * 单例实例
     */
    private static final IdGeneratorMonitor INSTANCE = new IdGeneratorMonitor();

    /**
     * 监控的ID生成器集合
     */
    private final Map<String, IdGenerator> generators = new ConcurrentHashMap<>();

    /**
     * 监控统计数据
     */
    private final Map<String, GeneratorStat> stats = new ConcurrentHashMap<>();

    /**
     * 私有构造函数
     */
    private IdGeneratorMonitor() {
        // 启动统计线程
        startStatThread();
    }

    /**
     * 获取实例
     *
     * @return 监控实例
     */
    public static IdGeneratorMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * 注册ID生成器
     *
     * @param generator ID生成器
     */
    public void register(IdGenerator generator) {
        String name = generator.getName();
        generators.put(name, generator);
        stats.putIfAbsent(name, new GeneratorStat(name));
        log.info("ID生成器[{}]已注册到监控", name);
    }

    /**
     * 记录ID生成成功
     *
     * @param generatorName 生成器名称
     */
    public void recordSuccess(String generatorName) {
        GeneratorStat stat = stats.get(generatorName);
        if (stat != null) {
            stat.getSuccessCount().incrementAndGet();
        }
    }

    /**
     * 记录ID生成失败
     *
     * @param generatorName 生成器名称
     */
    public void recordFailure(String generatorName) {
        GeneratorStat stat = stats.get(generatorName);
        if (stat != null) {
            stat.getFailureCount().incrementAndGet();
        }
    }

    /**
     * 记录ID生成耗时
     *
     * @param generatorName 生成器名称
     * @param costMs        耗时（毫秒）
     */
    public void recordTime(String generatorName, long costMs) {
        GeneratorStat stat = stats.get(generatorName);
        if (stat != null) {
            stat.getTotalTime().addAndGet(costMs);
            stat.getCallCount().incrementAndGet();

            // 更新最大耗时
            synchronized (stat) {
                if (costMs > stat.getMaxTime()) {
                    stat.setMaxTime(costMs);
                }
            }
        }
    }

    /**
     * 获取所有监控统计数据
     *
     * @return 统计数据Map
     */
    public Map<String, GeneratorStat> getStats() {
        // 返回统计数据的复制，防止外部修改
        Map<String, GeneratorStat> result = new HashMap<>(stats.size());
        for (Map.Entry<String, GeneratorStat> entry : stats.entrySet()) {
            result.put(entry.getKey(), entry.getValue().copy());
        }
        return result;
    }

    /**
     * 启动统计线程，每分钟记录一次日志
     */
    private void startStatThread() {
        Thread statThread = new Thread(() -> {
            while (true) {
                try {
                    // 每分钟记录一次日志
                    Thread.sleep(60 * 1000);
                    logStats();
                } catch (InterruptedException e) {
                    log.warn("监控统计线程被中断", e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("监控统计异常", e);
                }
            }
        });
        statThread.setName("easy-id-monitor");
        statThread.setDaemon(true);
        statThread.start();
    }

    /**
     * 记录统计日志
     */
    private void logStats() {
        for (GeneratorStat stat : stats.values()) {
            long callCount = stat.getCallCount().get();
            if (callCount > 0) {
                long totalTime = stat.getTotalTime().get();
                double avgTime = callCount > 0 ? (double) totalTime / callCount : 0;
                log.info("ID生成器[{}]统计: 调用次数={}, 成功次数={}, 失败次数={}, 平均耗时={}ms, 最大耗时={}ms",
                        stat.getGeneratorName(),
                        callCount,
                        stat.getSuccessCount().get(),
                        stat.getFailureCount().get(),
                        String.format("%.2f", avgTime),
                        stat.getMaxTime());
            }
        }
    }

    /**
     * 生成器统计数据
     */
    @Data
    public static class GeneratorStat {
        /**
         * 生成器名称
         */
        private final String generatorName;

        /**
         * 调用次数
         */
        private final AtomicLong callCount = new AtomicLong(0);

        /**
         * 成功次数
         */
        private final AtomicLong successCount = new AtomicLong(0);

        /**
         * 失败次数
         */
        private final AtomicLong failureCount = new AtomicLong(0);

        /**
         * 总耗时（毫秒）
         */
        private final AtomicLong totalTime = new AtomicLong(0);

        /**
         * 最大耗时（毫秒）
         */
        private volatile long maxTime = 0;

        /**
         * 构造函数
         *
         * @param generatorName 生成器名称
         */
        public GeneratorStat(String generatorName) {
            this.generatorName = generatorName;
        }

        /**
         * 复制统计数据
         *
         * @return 复制的统计数据
         */
        public GeneratorStat copy() {
            GeneratorStat copy = new GeneratorStat(this.generatorName);
            copy.getCallCount().set(this.callCount.get());
            copy.getSuccessCount().set(this.successCount.get());
            copy.getFailureCount().set(this.failureCount.get());
            copy.getTotalTime().set(this.totalTime.get());
            copy.setMaxTime(this.maxTime);
            return copy;
        }
    }
}