package com.easy.id.core.model;

import lombok.Data;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 号段缓冲区（双Buffer）
 */
@Data
public class SegmentBuffer {

    /**
     * 业务键
     */
    private String businessKey;

    /**
     * 双Buffer
     */
    private Segment[] segments = new Segment[2];

    /**
     * 当前使用的segment的索引
     */
    private volatile int currentIndex = 0;

    /**
     * 是否处于下一个segment加载中
     */
    private AtomicBoolean isLoadingNext = new AtomicBoolean(false);

    /**
     * 初始化标记
     */
    private volatile boolean initialized = false;

    /**
     * 步长
     */
    private int step = 1000;

    /**
     * 获取当前号段
     */
    public Segment getCurrent() {
        return segments[currentIndex];
    }

    /**
     * 获取下一个号段
     */
    public Segment getNextSegment() {
        return segments[nextIndex()];
    }

    /**
     * 切换到下一个号段
     */
    public void switchToNext() {
        currentIndex = nextIndex();
    }

    /**
     * 获取下一个号段的索引
     */
    private int nextIndex() {
        return (currentIndex + 1) % 2;
    }
}