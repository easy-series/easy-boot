package com.easy.id.utils;

import com.easy.id.core.IdGenerator;
import com.easy.id.exception.IdGeneratorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ID生成器工具类
 */
@Slf4j
@Component
public class IdGeneratorUtil {

    private static IdGenerator idGenerator;

    @Autowired
    public void setIdGenerator(IdGenerator idGenerator) {
        IdGeneratorUtil.idGenerator = idGenerator;
    }

    /**
     * 获取下一个ID
     *
     * @return ID
     */
    public static long nextId() {
        if (idGenerator == null) {
            throw new IdGeneratorException("IdGenerator未初始化");
        }
        return idGenerator.nextId();
    }

    /**
     * 获取下一个ID（带业务标识）
     *
     * @param businessKey 业务标识
     * @return ID
     */
    public static long nextId(String businessKey) {
        if (idGenerator == null) {
            throw new IdGeneratorException("IdGenerator未初始化");
        }
        return idGenerator.nextId(businessKey);
    }

    /**
     * 解析ID中的时间戳
     *
     * @param id ID
     * @return 时间戳（毫秒）
     */
    public static long parseTime(long id) {
        if (idGenerator == null) {
            throw new IdGeneratorException("IdGenerator未初始化");
        }
        return idGenerator.parseTime(id);
    }

    /**
     * 解析ID中的序列号
     *
     * @param id ID
     * @return 序列号
     */
    public static long parseSequence(long id) {
        if (idGenerator == null) {
            throw new IdGeneratorException("IdGenerator未初始化");
        }
        return idGenerator.parseSequence(id);
    }
}