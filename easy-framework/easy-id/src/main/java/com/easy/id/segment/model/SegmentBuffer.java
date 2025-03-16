package com.easy.id.segment.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 号段缓冲区，实现双Buffer机制
 * 每个业务Key对应一个SegmentBuffer，每个SegmentBuffer包含两个Segment
 *
 * @author 芋道源码
 */
@Data
@Slf4j
public class SegmentBuffer {

    /**
     * 业务标识
     */
    private String bizKey;

    /**
     * 双缓冲区，索引0和1
     */
    private final Segment[] segments = new Segment[] { new Segment(), new Segment() };

    /**
     * 当前使用的segment索引
     */
    private volatile int currentPos = 0;

    /**
     * 下一个segment是否处于正在加载状态
     */
    private final AtomicBoolean isLoadingNext = new AtomicBoolean(false);

    /**
     * 初始化标记，避免重复初始化
     */
    private volatile boolean initialized = false;

    /**
     * 读写锁，用于保护关键操作
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    /**
     * 步长
     */
    private volatile int step = 1000;

    /**
     * 最小步长
     */
    private volatile int minStep = 1000;

    /**
     * 最大步长
     */
    private volatile int maxStep = 100000;

    /**
     * 更新时间戳
     */
    private volatile long updateTimestamp = System.currentTimeMillis();

    /**
     * 获取当前使用的号段
     *
     * @return 当前号段
     */
    public Segment getCurrent() {
        return segments[currentPos];
    }

    /**
     * 获取下一个备用号段
     *
     * @return 下一个号段
     */
    public Segment getNext() {
        return segments[currentPos == 0 ? 1 : 0];
    }

    /**
     * 切换到下一个号段
     */
    public void switchPos() {
        currentPos = currentPos == 0 ? 1 : 0;
    }

    /**
     * 判断是否需要加载下一个号段
     * 
     * @return 是否需要加载下一个号段
     */
    public boolean needLoadNext() {
        Segment current = getCurrent();
        long currentValue = current.getValue().get();
        long threshold = current.getMax() - current.getStep() * current.getLoadingPercent() / 100;
        return currentValue >= threshold && isLoadingNext.compareAndSet(false, true);
    }

    /**
     * 初始化Buffer
     *
     * @param bizKey 业务标识
     * @param step   步长
     */
    public void init(String bizKey, int step) {
        if (initialized) {
            return;
        }
        writeLock.lock();
        try {
            if (initialized) {
                return;
            }
            this.bizKey = bizKey;
            this.step = step > 0 ? step : this.step;
            initialized = true;
        } finally {
            writeLock.unlock();
        }
    }
}