package com.easy.id.segment.dao;

/**
 * 号段分配器接口
 * 
 * 负责从数据源获取号段，不同的存储实现可以有不同的分配器实现
 *
 * @author 芋道源码
 */
public interface SegmentAllocator {

    /**
     * 获取下一个号段范围
     *
     * @param bizKey 业务标识
     * @param step 步长
     * @return 号段范围
     */
    SegmentRange nextRange(String bizKey, int step);
} 