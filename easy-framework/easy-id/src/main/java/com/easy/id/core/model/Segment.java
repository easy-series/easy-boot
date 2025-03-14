package com.easy.id.core.model;

import java.util.concurrent.atomic.AtomicLong;

import lombok.Data;

/**
 * 号段模型
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
     * 是否用完
     */
    public boolean isExhausted() {
        return currentValue.get() >= maxValue;
    }

    /**
     * 获取下一个值
     */
    public long getNextValue() {
        return currentValue.incrementAndGet();
    }

    /**
     * 获取当前进度百分比
     */
    public int getLoadingPercent() {
        if (step == 0) {
            return 100;
        }
        long currentVal = currentValue.get();
        long startVal = maxValue - step;
        return (int) (((currentVal - startVal) / (double) step) * 100);
    }
}