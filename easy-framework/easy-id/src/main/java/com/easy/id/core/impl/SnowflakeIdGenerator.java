package com.easy.id.core.impl;

import com.easy.id.core.AbstractSnowflakeIdGenerator;
import com.easy.id.exception.IdGeneratorException;
import lombok.extern.slf4j.Slf4j;

/**
 * 雪花算法ID生成器实现
 */
@Slf4j
public class SnowflakeIdGenerator extends AbstractSnowflakeIdGenerator {

    public SnowflakeIdGenerator(long workerId) {
        super(workerId);
    }

    @Override
    public long nextId(String businessKey) {
        // 雪花算法不支持业务键，直接调用无参方法
        return nextId();
    }
} 