package com.easy.id.snowflake;

import com.easy.id.core.AbstractIdGenerator;
import com.easy.id.exception.IdGeneratorException;
import lombok.extern.slf4j.Slf4j;

/**
 * 雪花算法ID生成器实现
 * 
 * 参考自Twitter的Snowflake算法，使用41位表示毫秒级时间戳，10位表示机器ID（5位数据中心+5位工作机器），
 * 12位表示序列号，总共64位，可以有效避免ID冲突，且趋势递增，适合分布式环境。
 *
 * @author 芋道源码
 */
@Slf4j
public class SnowflakeIdGenerator extends AbstractIdGenerator {

    /**
     * 开始时间截 (2020-01-01)，用于计算时间戳，减少时间戳位数
     */
    private final static long EPOCH = 1577808000000L;

    /**
     * 机器ID所占的位数
     */
    private final static long WORKER_ID_BITS = 5L;

    /**
     * 数据中心ID所占的位数
     */
    private final static long DATA_CENTER_ID_BITS = 5L;

    /**
     * 支持的最大机器ID，结果是31 (这个移位算法可以快速计算出几位二进制数所能表示的最大十进制数)
     */
    private final static long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 支持的最大数据中心ID，结果是31
     */
    private final static long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    /**
     * 序列在ID中占的位数
     */
    private final static long SEQUENCE_BITS = 12L;

    /**
     * 机器ID向左移12位
     */
    private final static long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心ID向左移17位(12+5)
     */
    private final static long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间截向左移22位(5+5+12)
     */
    private final static long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    private final static long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 工作机器ID(0~31)
     */
    private final long workerId;

    /**
     * 数据中心ID(0~31)
     */
    private final long dataCenterId;

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     *
     * @param name 生成器名称
     * @param workerId 工作ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     */
    public SnowflakeIdGenerator(String name, long workerId, long dataCenterId) {
        super(name);
        
        // 检查参数合法性
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker ID can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException(String.format("DataCenter ID can't be greater than %d or less than 0", MAX_DATA_CENTER_ID));
        }
        
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
        
        log.info("SnowflakeIdGenerator initialized with workerId: {}, dataCenterId: {}", workerId, dataCenterId);
    }

    /**
     * 获取下一个ID (线程安全)
     *
     * @return SnowflakeId
     */
    @Override
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 如果时钟回退在5毫秒内，等待至上次生成ID的时间
                try {
                    Thread.sleep(offset);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new IdGeneratorException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
                    }
                } catch (Exception e) {
                    throw new IdGeneratorException(e);
                }
            } else {
                throw new IdGeneratorException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
            }
        }

        // 如果是同一毫秒内生成的，则进行序列递增
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 同一毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        // 更新上次生成ID的时间截
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }
} 