package com.easy.id.core.model;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 号段（Segment）模型
 */
@Data
public class Segment {

    /**
     * 业务键
     */
    private String businessKey;

    /**
     * 当前值
     */
    private AtomicLong currentValue;

    /**
     * 步长
     */
    private int step;

    /**
     * 最大值
     */
    private long maxValue;

    /**
     * 获取号段已经使用的百分比
     */
    public double getUsedPercent() {
        long current = currentValue.get();
        if (current >= maxValue) {
            return 100.0;
        }
        return (double) (current - (maxValue - step)) / step * 100;
    }

    /**
     * 是否需要加载下一个号段
     */
    public boolean isNeedToLoadNext() {
        return getUsedPercent() > 70.0;
    }

    /**
     * 是否用尽
     */
    public boolean isExhausted() {
        return currentValue.get() >= maxValue;
    }

    /**
     * 获取下一个值
     */
    public long nextValue() {
        return currentValue.incrementAndGet();
    }
}