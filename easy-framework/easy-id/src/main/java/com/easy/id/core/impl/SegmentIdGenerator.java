package com.easy.id.core.impl;

import com.easy.id.core.IdGenerator;
import com.easy.id.core.model.Segment;
import com.easy.id.core.model.SegmentBuffer;
import com.easy.id.core.service.SegmentService;
import com.easy.id.exception.IdGeneratorException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 基于号段模式的ID生成器
 */
@Slf4j
public class SegmentIdGenerator implements IdGenerator {

    /**
     * 默认业务键
     */
    private static final String DEFAULT_BUSINESS_KEY = "default";

    /**
     * 号段服务
     */
    private final SegmentService segmentService;

    /**
     * 业务键与号段缓冲区的映射
     */
    private final Map<String, SegmentBuffer> businessKeyBuffers = new ConcurrentHashMap<>();

    /**
     * 用于异步加载下一个号段的线程池
     */
    private final ExecutorService executorService;

    /**
     * 默认步长
     */
    private final int step;

    public SegmentIdGenerator(SegmentService segmentService, int step) {
        this.segmentService = segmentService;
        this.step = step;
        this.executorService = new ThreadPoolExecutor(
                2,
                5,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        // 初始化默认业务键
        init(DEFAULT_BUSINESS_KEY, step);
    }

    /**
     * 初始化业务键
     */
    public void init(String businessKey, int step) {
        // 初始化号段缓冲区
        SegmentBuffer buffer = new SegmentBuffer();
        buffer.setBusinessKey(businessKey);
        buffer.setStep(step > 0 ? step : this.step);
        businessKeyBuffers.put(businessKey, buffer);

        // 初始化业务键
        segmentService.initBusinessKey(businessKey, buffer.getStep());
    }

    @Override
    public long nextId() {
        return nextId(DEFAULT_BUSINESS_KEY);
    }

    @Override
    public long nextId(String businessKey) {
        if (businessKey == null || businessKey.isEmpty()) {
            businessKey = DEFAULT_BUSINESS_KEY;
        }

        // 获取或初始化号段缓冲区
        SegmentBuffer buffer = businessKeyBuffers.get(businessKey);
        if (buffer == null) {
            synchronized (this) {
                buffer = businessKeyBuffers.get(businessKey);
                if (buffer == null) {
                    init(businessKey, step);
                    buffer = businessKeyBuffers.get(businessKey);
                }
            }
        }

        return getIdFromSegmentBuffer(buffer);
    }

    private long getIdFromSegmentBuffer(SegmentBuffer buffer) {
        // 如果缓冲区未初始化，则初始化
        if (!buffer.isInitialized()) {
            synchronized (buffer) {
                if (!buffer.isInitialized()) {
                    loadSegment(buffer, 0);
                    buffer.setInitialized(true);
                }
            }
        }

        // 获取当前号段
        Segment segment = buffer.getCurrent();
        if (segment == null) {
            throw new IdGeneratorException("当前号段为空");
        }

        // 当前号段使用率超过阈值，异步加载下一个号段
        if (segment.isNeedToLoadNext() && buffer.getIsLoadingNext().compareAndSet(false, true)) {
            executorService.execute(() -> {
                try {
                    loadSegment(buffer, buffer.getCurrentIndex() == 0 ? 1 : 0);
                } catch (Exception e) {
                    log.error("加载下一个号段失败", e);
                } finally {
                    buffer.getIsLoadingNext().set(false);
                }
            });
        }

        // 当前号段已耗尽，切换到下一个号段
        if (segment.isExhausted()) {
            synchronized (buffer) {
                // 再次检查是否耗尽，避免并发问题
                if (segment.isExhausted()) {
                    // 切换到下一个号段
                    buffer.switchToNext();
                    segment = buffer.getCurrent();
                    // 如果下一个号段也为空或耗尽，则同步加载
                    if (segment == null || segment.isExhausted()) {
                        loadSegment(buffer, buffer.getCurrentIndex());
                        segment = buffer.getCurrent();
                    }
                }
            }
        }

        // 获取下一个ID
        return segment.nextValue();
    }

    /**
     * 加载号段
     */
    private void loadSegment(SegmentBuffer buffer, int index) {
        Segment segment = segmentService.getNextSegment(buffer.getBusinessKey(), buffer.getStep());
        buffer.getSegments()[index] = segment;
    }

    @Override
    public long parseTime(long id) {
        // 号段模式生成的ID不包含时间戳，返回0
        return 0;
    }

    @Override
    public long parseSequence(long id) {
        // 号段模式生成的ID本身就是序列，直接返回
        return id;
    }
}