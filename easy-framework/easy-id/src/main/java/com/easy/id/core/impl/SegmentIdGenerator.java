package com.easy.id.core.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.easy.id.core.IdGenerator;
import com.easy.id.core.model.Segment;
import com.easy.id.core.service.SegmentService;
import com.easy.id.exception.IdGeneratorException;

import lombok.extern.slf4j.Slf4j;

/**
 * 号段ID生成器
 * 基于美团Leaf-Segment设计
 */
@Slf4j
public class SegmentIdGenerator implements IdGenerator {

    /**
     * 号段服务
     */
    private final SegmentService segmentService;

    /**
     * 默认步长
     */
    private final int step;

    /**
     * 线程池
     */
    private final ExecutorService executorService;

    /**
     * 双缓冲区
     */
    private final Map<String, SegmentBuffer> segmentBufferMap = new ConcurrentHashMap<>();

    /**
     * 构造方法
     *
     * @param segmentService 号段服务
     * @param step           步长
     */
    public SegmentIdGenerator(SegmentService segmentService, int step) {
        this.segmentService = segmentService;
        this.step = step;
        // 创建线程池
        this.executorService = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("segment-id-generator");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy());
        log.info("初始化号段ID生成器，步长: {}", step);
    }

    @Override
    public long nextId() {
        // 默认业务键
        return nextId("default");
    }

    @Override
    public long nextId(String businessKey) {
        // 获取或创建号段缓冲区
        SegmentBuffer buffer = segmentBufferMap.computeIfAbsent(businessKey, k -> new SegmentBuffer(businessKey));

        return getNextId(buffer);
    }

    @Override
    public long parseTime(long id) {
        // 号段模式下不支持解析时间
        return 0;
    }

    @Override
    public long parseSequence(long id) {
        // 号段模式下返回原始ID
        return id;
    }

    /**
     * 获取下一个ID
     */
    private long getNextId(SegmentBuffer buffer) {
        while (true) {
            // 获取当前活跃的号段
            Segment segment = buffer.getCurrent();

            // 如果当前没有可用号段，则需要初始化
            if (segment == null) {
                synchronized (buffer) {
                    if (buffer.getCurrent() == null) {
                        // 从服务获取一个新号段
                        segment = segmentService.getNextSegment(buffer.getBusinessKey(), step);
                        buffer.setCurrent(segment);
                    } else {
                        // 已被其他线程初始化
                        segment = buffer.getCurrent();
                    }
                }
            }

            // 尝试获取ID
            long value = segment.getNextValue();

            // 如果当前号段还未用完，直接返回
            if (value < segment.getMaxValue()) {
                // 如果当前号段使用量超过阈值，触发异步加载下一个号段
                if (segment.getLoadingPercent() >= 80 && buffer.getNext() == null && !buffer.isLoadingNext()) {
                    asyncLoadSegment(buffer);
                }
                return value;
            }

            // 当前号段已用完，切换到下一个号段
            if (buffer.getNext() == null) {
                // 如果下一个号段还未加载完成，同步等待
                synchronized (buffer) {
                    if (buffer.getNext() == null) {
                        try {
                            // 从服务获取一个新号段
                            Segment nextSegment = segmentService.getNextSegment(buffer.getBusinessKey(), step);
                            buffer.setNext(nextSegment);
                            buffer.switchToNext();
                        } catch (Exception e) {
                            log.error("获取下一个号段失败", e);
                            throw new IdGeneratorException("获取ID失败: " + e.getMessage(), e);
                        }
                    } else {
                        // 已被其他线程加载完成，直接切换
                        buffer.switchToNext();
                    }
                }
            } else {
                // 下一个号段已加载完成，直接切换
                buffer.switchToNext();
            }
        }
    }

    /**
     * 异步加载下一个号段
     */
    private void asyncLoadSegment(SegmentBuffer buffer) {
        buffer.setLoadingNext(true);
        executorService.submit(() -> {
            try {
                // 从服务获取一个新号段
                Segment nextSegment = segmentService.getNextSegment(buffer.getBusinessKey(), step);
                buffer.setNext(nextSegment);
                log.info("异步加载号段成功: {}", buffer.getBusinessKey());
            } catch (Exception e) {
                log.error("异步加载号段失败", e);
            } finally {
                buffer.setLoadingNext(false);
            }
        });
    }

    /**
     * 号段缓冲区
     * 用于实现双Buffer
     */
    private static class SegmentBuffer {
        /**
         * 业务键
         */
        private final String businessKey;

        /**
         * 当前号段
         */
        private volatile Segment current;

        /**
         * 下一个号段
         */
        private volatile Segment next;

        /**
         * 是否正在加载下一个号段
         */
        private volatile boolean loadingNext = false;

        public SegmentBuffer(String businessKey) {
            this.businessKey = businessKey;
        }

        public String getBusinessKey() {
            return businessKey;
        }

        public Segment getCurrent() {
            return current;
        }

        public void setCurrent(Segment current) {
            this.current = current;
        }

        public Segment getNext() {
            return next;
        }

        public void setNext(Segment next) {
            this.next = next;
        }

        public boolean isLoadingNext() {
            return loadingNext;
        }

        public void setLoadingNext(boolean loadingNext) {
            this.loadingNext = loadingNext;
        }

        /**
         * 切换到下一个号段
         */
        public void switchToNext() {
            this.current = this.next;
            this.next = null;
        }
    }
}