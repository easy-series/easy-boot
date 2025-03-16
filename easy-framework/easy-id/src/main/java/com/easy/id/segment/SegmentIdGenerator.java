package com.easy.id.segment;

import com.easy.id.core.AbstractIdGenerator;
import com.easy.id.exception.IdGeneratorException;
import com.easy.id.segment.dao.SegmentAllocator;
import com.easy.id.segment.dao.SegmentRange;
import com.easy.id.segment.model.Segment;
import com.easy.id.segment.model.SegmentBuffer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 号段模式ID生成器实现
 * 
 * 基于Leaf号段模式实现，通过数据库或其他方式分配号段，使用双Buffer机制提高性能
 * 
 * @author 芋道源码
 */
@Slf4j
public class SegmentIdGenerator extends AbstractIdGenerator {

    /**
     * 号段分配器，负责从数据库或其他方式分配号段
     */
    private final SegmentAllocator segmentAllocator;

    /**
     * 业务缓冲区映射表，每个业务Key对应一个缓冲区
     */
    private final Map<String, SegmentBuffer> cache = new ConcurrentHashMap<>();

    /**
     * 线程池，用于异步加载号段
     */
    private ExecutorService service;

    /**
     * 默认业务Key
     */
    private static final String DEFAULT_BIZ_KEY = "default";

    /**
     * 构造函数
     *
     * @param name             生成器名称
     * @param segmentAllocator 号段分配器
     */
    public SegmentIdGenerator(String name, SegmentAllocator segmentAllocator) {
        super(name);
        if (segmentAllocator == null) {
            throw new IllegalArgumentException("SegmentAllocator cannot be null");
        }
        this.segmentAllocator = segmentAllocator;

        // 初始化线程池，核心线程数为处理器核心数，最大线程数为核心数*2
        int processors = Runtime.getRuntime().availableProcessors();
        this.service = new ThreadPoolExecutor(processors, processors * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5000),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("easy-id-segment-loader-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy());
        log.info("SegmentIdGenerator initialized with name: {}", name);

        // 预初始化默认业务键的缓冲区
        try {
            initBuffer(DEFAULT_BIZ_KEY);
            log.info("Default buffer initialized for bizKey: {}", DEFAULT_BIZ_KEY);
        } catch (Exception e) {
            log.warn("Failed to initialize default buffer: {}", e.getMessage());
        }
    }

    /**
     * 初始化业务键的缓冲区
     * 
     * @param bizKey 业务键
     * @return 初始化的缓冲区
     */
    private SegmentBuffer initBuffer(String bizKey) {
        // 如果缓冲区已存在，直接返回
        SegmentBuffer existingBuffer = cache.get(bizKey);
        if (existingBuffer != null && existingBuffer.isInitialized()) {
            return existingBuffer;
        }

        // 创建新的缓冲区
        SegmentBuffer buffer = new SegmentBuffer();
        buffer.init(bizKey, 0);

        // 初始化第一个号段
        try {
            log.info("Loading first segment for bizKey: {}", bizKey);
            SegmentRange range = segmentAllocator.nextRange(bizKey, buffer.getStep());
            log.info("Loaded segment range: min={}, max={}, step={}", range.getMin(), range.getMax(), range.getStep());

            Segment segment = buffer.getCurrent();
            segment.init(range.getMin(), range.getMax(), range.getStep());
            buffer.setInitialized(true);

            // 将初始化好的缓冲区放入缓存
            cache.put(bizKey, buffer);
            log.info("Buffer initialized and cached for bizKey: {}", bizKey);

            return buffer;
        } catch (Exception e) {
            log.error("Failed to initialize buffer for bizKey: {}, error: {}", bizKey, e.getMessage(), e);
            throw new IdGeneratorException("初始化号段失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取下一个ID
     *
     * @return 下一个ID
     */
    @Override
    public long nextId() {
        return nextId(DEFAULT_BIZ_KEY);
    }

    /**
     * 获取指定业务的下一个ID
     *
     * @param bizKey 业务标识
     * @return 下一个ID
     */
    public long nextId(String bizKey) {
        if (bizKey == null || bizKey.trim().isEmpty()) {
            bizKey = DEFAULT_BIZ_KEY;
        }

        try {
            // 如果业务缓冲区不存在，则初始化
            if (!cache.containsKey(bizKey)) {
                synchronized (this) {
                    if (!cache.containsKey(bizKey)) {
                        log.info("Buffer not found for bizKey: {}, initializing...", bizKey);
                        initBuffer(bizKey);
                    }
                }
            }

            SegmentBuffer buffer = cache.get(bizKey);
            if (buffer == null) {
                log.error("Failed to get buffer for bizKey: {}", bizKey);
                throw new IdGeneratorException("无法获取业务键的缓冲区: " + bizKey);
            }

            if (!buffer.isInitialized()) {
                synchronized (buffer) {
                    if (!buffer.isInitialized()) {
                        log.info("Buffer not initialized for bizKey: {}, initializing...", bizKey);
                        // 初始化第一个号段
                        try {
                            SegmentRange range = segmentAllocator.nextRange(bizKey, buffer.getStep());
                            log.info("Loaded segment range: min={}, max={}, step={}",
                                    range.getMin(), range.getMax(), range.getStep());

                            Segment segment = buffer.getCurrent();
                            segment.init(range.getMin(), range.getMax(), range.getStep());
                            buffer.setInitialized(true);
                            log.info("Buffer initialized for bizKey: {}", bizKey);
                        } catch (Exception e) {
                            log.error("初始化号段失败，bizKey: {}", bizKey, e);
                            throw new IdGeneratorException("初始化号段失败: " + e.getMessage(), e);
                        }
                    }
                }
            }

            return getIdFromSegmentBuffer(buffer, bizKey);
        } catch (IdGeneratorException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取ID时发生异常，bizKey: {}", bizKey, e);
            throw new IdGeneratorException("获取ID时发生异常: " + e.getMessage(), e);
        }
    }

    /**
     * 从缓冲区获取ID
     * 
     * @param buffer 缓冲区
     * @param bizKey 业务标识
     * @return ID
     */
    private long getIdFromSegmentBuffer(SegmentBuffer buffer, String bizKey) {
        if (buffer == null) {
            log.error("Buffer is null for bizKey: {}", bizKey);
            throw new IdGeneratorException("业务缓冲区为空: " + bizKey);
        }

        buffer.getReadLock().lock();
        try {
            final Segment segment = buffer.getCurrent();
            if (segment == null) {
                log.error("Current segment is null for bizKey: {}", bizKey);
                throw new IdGeneratorException("当前号段为空: " + bizKey);
            }

            // 如果当前号段已经用完，则尝试切换到下一个号段
            if (segment.isOver()) {
                buffer.getReadLock().unlock();
                // 获取写锁进行切换
                buffer.getWriteLock().lock();
                try {
                    // 再次检查，避免重复切换
                    if (buffer.getCurrent().isOver()) {
                        // 如果下一个号段准备好了，直接切换
                        Segment next = buffer.getNext();
                        if (next == null || !next.isInitialized()) {
                            log.warn("Next segment not ready for bizKey: {}, loading synchronously", bizKey);
                            // 如果下一个号段没准备好，同步加载
                            SegmentRange range = segmentAllocator.nextRange(bizKey, buffer.getStep());
                            next = new Segment();
                            next.init(range.getMin(), range.getMax(), range.getStep());
                            // 设置到buffer中
                            buffer.getSegments()[buffer.getCurrentPos() == 0 ? 1 : 0] = next;
                        }
                        buffer.switchPos();
                        log.info("Switched to next segment for bizKey: {}", bizKey);
                        buffer.getReadLock().lock();
                        buffer.getWriteLock().unlock();
                    } else {
                        // 当前号段未用完，释放写锁，获取读锁
                        buffer.getReadLock().lock();
                        buffer.getWriteLock().unlock();
                    }
                } catch (Exception e) {
                    buffer.getWriteLock().unlock();
                    log.error("切换号段失败，bizKey: {}", bizKey, e);
                    throw new IdGeneratorException("切换号段失败: " + e.getMessage(), e);
                }
            }

            // 获取下一个ID
            long id = segment.nextId();
            if (id == -1) {
                log.warn("Segment is exhausted for bizKey: {}, segment range: {}-{}",
                        bizKey, segment.getValue().get() - 1, segment.getMax());
                // 号段已用尽，递归调用获取ID
                return nextId(bizKey);
            }

            // 如果下一个号段还没准备好，并且当前号段使用量到达阈值，则触发异步加载
            if (buffer.needLoadNext()) {
                log.info("Threshold reached, loading next segment asynchronously for bizKey: {}", bizKey);
                service.submit(new SegmentLoader(bizKey, buffer, segmentAllocator));
            }

            return id;
        } finally {
            if (buffer.getReadLock().tryLock()) {
                buffer.getReadLock().unlock();
            }
        }
    }

    /**
     * 批量获取ID
     *
     * @param count 要获取的ID数量
     * @return ID数组
     */
    @Override
    public long[] nextId(int count) {
        return nextId(DEFAULT_BIZ_KEY, count);
    }

    /**
     * 批量获取指定业务的ID
     *
     * @param bizKey 业务标识
     * @param count  要获取的ID数量
     * @return ID数组
     */
    public long[] nextId(String bizKey, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }

        if (bizKey == null || bizKey.trim().isEmpty()) {
            bizKey = DEFAULT_BIZ_KEY;
        }

        long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            ids[i] = nextId(bizKey);
        }
        return ids;
    }

    /**
     * 号段加载器，异步加载下一个号段
     */
    private static class SegmentLoader implements Runnable {

        private final String bizKey;
        private final SegmentBuffer buffer;
        private final SegmentAllocator allocator;

        public SegmentLoader(String bizKey, SegmentBuffer buffer, SegmentAllocator allocator) {
            this.bizKey = bizKey;
            this.buffer = buffer;
            this.allocator = allocator;
        }

        @Override
        public void run() {
            try {
                // 从分配器获取下一个号段范围
                SegmentRange range = allocator.nextRange(bizKey, buffer.getStep());
                log.debug("Async loaded next segment range: bizKey={}, min={}, max={}, step={}",
                        bizKey, range.getMin(), range.getMax(), range.getStep());

                // 获取写锁，更新下一个号段
                buffer.getWriteLock().lock();
                try {
                    Segment next = buffer.getNext();
                    if (next == null) {
                        next = new Segment();
                        buffer.getSegments()[buffer.getCurrentPos() == 0 ? 1 : 0] = next;
                    }
                    next.init(range.getMin(), range.getMax(), range.getStep());
                } finally {
                    buffer.getWriteLock().unlock();
                }

                // 重置加载状态
                buffer.getIsLoadingNext().set(false);
                log.debug("异步加载完成下一个号段，bizKey: {}, min: {}, max: {}", bizKey, range.getMin(), range.getMax());
            } catch (Exception e) {
                // 加载异常，设置状态为未加载，允许下次重试
                buffer.getIsLoadingNext().set(false);
                log.error("异步加载号段异常，bizKey: {}", bizKey, e);
            }
        }
    }

    /**
     * 关闭生成器，释放资源
     */
    public void shutdown() {
        if (service != null) {
            service.shutdown();
            try {
                service.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("关闭线程池被中断", e);
                Thread.currentThread().interrupt();
            }
            service = null;
        }
    }
}
