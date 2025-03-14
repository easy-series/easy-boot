package com.easy.id.core.impl;

import com.easy.id.core.AbstractSnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;

/**
 * 雪花算法ID生成器实现类
 */
@Slf4j
public class SnowflakeIdGenerator extends AbstractSnowflakeIdGenerator {

    /**
     * 构造方法
     *
     * @param workerId 机器ID
     */
    public SnowflakeIdGenerator(long workerId) {
        super(workerId);
    }
} 