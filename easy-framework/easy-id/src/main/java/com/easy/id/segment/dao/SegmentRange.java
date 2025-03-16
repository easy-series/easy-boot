package com.easy.id.segment.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 号段范围，表示一个号段的起始值、结束值和步长
 *
 * @author 芋道源码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SegmentRange {

    /**
     * 号段最小值（包含）
     */
    private long min;

    /**
     * 号段最大值（包含）
     */
    private long max;

    /**
     * 步长
     */
    private int step;

    /**
     * 获取号段的ID数量
     *
     * @return ID数量
     */
    public long getIdCount() {
        return max - min + 1;
    }
}