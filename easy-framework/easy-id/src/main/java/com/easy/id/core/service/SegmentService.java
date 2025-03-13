package com.easy.id.core.service;

import com.easy.id.core.model.Segment;

/**
 * 号段服务接口
 */
public interface SegmentService {

    /**
     * 获取下一个号段
     *
     * @param businessKey 业务键
     * @param step 步长
     * @return 分配的号段
     */
    Segment getNextSegment(String businessKey, int step);

    /**
     * 初始化业务键
     *
     * @param businessKey 业务键
     * @param step 步长
     */
    void initBusinessKey(String businessKey, int step);
} 