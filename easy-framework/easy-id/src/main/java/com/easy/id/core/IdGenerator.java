package com.easy.id.core;

/**
 * ID生成器接口
 */
public interface IdGenerator {

    /**
     * 获取下一个ID
     *
     * @return 生成的ID
     */
    long nextId();

    /**
     * 获取下一个ID（带业务标识）
     *
     * @param businessKey 业务标识
     * @return 生成的ID
     */
    long nextId(String businessKey);

    /**
     * 解析ID中的时间戳
     *
     * @param id ID
     * @return 时间戳（毫秒）
     */
    long parseTime(long id);

    /**
     * 解析ID中的序列号
     *
     * @param id ID
     * @return 序列号
     */
    long parseSequence(long id);

} 