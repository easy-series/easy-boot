package com.easy.id.core;

/**
 * ID生成器接口，是分布式ID生成系统的核心接口
 *
 * @author 芋道源码
 */
public interface IdGenerator {

    /**
     * 获取下一个ID
     *
     * @return 下一个ID
     */
    long nextId();

    /**
     * 批量获取多个ID
     *
     * @param count 要获取的ID数量
     * @return ID数组
     */
    long[] nextId(int count);

    /**
     * 获取ID生成器的名称
     *
     * @return 生成器名称
     */
    String getName();
} 