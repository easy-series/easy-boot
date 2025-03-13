package com.easy.id.core;

import com.easy.id.exception.IdGeneratorException;
import lombok.extern.slf4j.Slf4j;

/**
 * 雪花算法ID生成器抽象类
 */
@Slf4j
public abstract class AbstractSnowflakeIdGenerator implements IdGenerator {

    /**
     * 开始时间戳（2023-01-01 00:00:00）
     */
    private final static long START_TIMESTAMP = 1672502400000L;

    /**
     * 序列号占用位数
     */
    private final static long SEQUENCE_BITS = 12L;

    /**
     * 机器ID占用位数
     */
    private final static long WORKER_ID_BITS = 10L;

    /**
     * 序列号最大值 (2^12-1 = 4095)
     */
    private final static long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器ID左移位数（序列号位数）
     */
    private final static long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 时间戳左移位数（机器ID位数 + 序列号位数）
     */
    private final static long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;

    /**
     * 最大机器ID (2^10-1 = 1023)
     */
    private final static long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 机器ID
     */
    private final long workerId;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    public AbstractSnowflakeIdGenerator(long workerId) {
        // 校验机器ID是否合法
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IdGeneratorException(
                    String.format("Worker ID can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        this.workerId = workerId;
        log.info("初始化雪花算法ID生成器，机器ID: {}", workerId);
    }

    @Override
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上次生成ID的时间戳，说明系统时钟回退，抛出异常
        if (timestamp < lastTimestamp) {
            log.error("时钟回退异常，上次时间: {}, 当前时间: {}", lastTimestamp, timestamp);
            throw new IdGeneratorException(String.format(
                    "Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        // 如果是同一时间生成的，则进行序列号自增
        if (lastTimestamp == timestamp) {
            // 序列号自增，通过与运算保证序列号不会超过最大值
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列号已经达到最大值，等待下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，序列号重置为0
            sequence = 0L;
        }

        // 更新上次生成ID的时间戳
        lastTimestamp = timestamp;

        // 组合ID: 时间戳部分 | 机器ID部分 | 序列号部分
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    @Override
    public long nextId(String businessKey) {
        // 默认实现不关心业务标识，直接调用无参方法
        return nextId();
    }

    @Override
    public long parseTime(long id) {
        return (id >> TIMESTAMP_SHIFT) + START_TIMESTAMP;
    }

    @Override
    public long parseSequence(long id) {
        return id & SEQUENCE_MASK;
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 等待下一个毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
}