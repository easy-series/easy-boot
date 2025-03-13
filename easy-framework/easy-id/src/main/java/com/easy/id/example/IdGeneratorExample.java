package com.easy.id.example;

import com.easy.id.core.IdGenerator;
import com.easy.id.utils.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * ID生成器示例
 */
@Slf4j
@Component
public class IdGeneratorExample {

    private final IdGenerator idGenerator;

    @Autowired
    public IdGeneratorExample(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * 通过注入的IdGenerator实例生成ID
     */
    public void generateIds() {
        // 生成ID
        long id1 = idGenerator.nextId();
        log.info("生成ID: {}", id1);

        // 生成带业务标识的ID
        long id2 = idGenerator.nextId("user");
        log.info("生成带业务标识的ID: {}", id2);

        // 解析时间戳（只对雪花算法有效）
        long timestamp = idGenerator.parseTime(id1);
        if (timestamp > 0) {
            log.info("解析的时间戳: {}, 对应时间: {}", timestamp, new Date(timestamp));
        }

        // 解析序列号
        long sequence = idGenerator.parseSequence(id1);
        log.info("解析的序列号: {}", sequence);
    }

    /**
     * 通过工具类生成ID
     */
    public void generateIdsWithUtil() {
        // 生成ID
        long id1 = IdGeneratorUtil.nextId();
        log.info("通过工具类生成ID: {}", id1);

        // 生成带业务标识的ID
        long id2 = IdGeneratorUtil.nextId("order");
        log.info("通过工具类生成带业务标识的ID: {}", id2);
    }
} 