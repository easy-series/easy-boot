package com.easy.id.segment.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 号段模型类，表示一个ID号段
 *
 * @author 芋道源码
 */
@Data
@Slf4j
public class Segment {

    /**
     * 最大ID值
     */
    private long max;

    /**
     * 当前值，使用AtomicLong保证线程安全
     */
    private AtomicLong value;

    /**
     * 步长
     */
    private int step;

    /**
     * 号段更新时间戳
     */
    private long updateTimestamp;

    /**
     * 初始化标记，避免加载号段时重复初始化
     */
    private volatile boolean initialized;

    /**
     * 是否被用完的标志
     */
    private volatile boolean over = false;

    /**
     * 阈值，用于判断何时触发异步加载下一个号段，一般为step的某个百分比
     */
    private volatile int loadingPercent = 20;

    /**
     * 构造函数，初始化默认值
     */
    public Segment() {
        // 初始化AtomicLong，防止空指针异常
        this.value = new AtomicLong(0);
        this.max = 0;
        this.step = 1000;
        this.initialized = false;
    }

    /**
     * 获取当前号段可用ID数量
     */
    public long getAvailableIdsCount() {
        return this.max - this.value.get();
    }

    /**
     * 获取号段的下一个ID
     */
    public long nextId() {
        // 确保value不为null
        if (value == null) {
            log.error("Segment value is null, initializing with default value");
            value = new AtomicLong(0);
            over = true;
            return -1;
        }

        long currentValue = value.getAndIncrement();
        if (currentValue > max) {
            over = true;
            return -1;
        }

        // 计算当前使用比例，如果达到预设阈值，返回特定值提示异步加载
        long currentCount = currentValue - (max - step);
        int currentPercent = (int) (currentCount * 100 / step);
        if (currentPercent >= 100 - loadingPercent) {
            log.debug("Segment {}-{} will be over soon, currentPercent: {}", max - step, max, currentPercent);
            return currentValue;
        }

        return currentValue;
    }

    /**
     * 初始化号段
     *
     * @param min  最小值
     * @param max  最大值
     * @param step 步长
     */
    public void init(long min, long max, int step) {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            // 确保初始化时value不为null
            if (this.value == null) {
                this.value = new AtomicLong(min);
            } else {
                this.value.set(min);
            }
            this.max = max;
            this.step = step;
            this.updateTimestamp = System.currentTimeMillis();
            this.initialized = true;
            this.over = false;
            log.info("Segment init: min={}, max={}, step={}", min, max, step);
        }
    }
}