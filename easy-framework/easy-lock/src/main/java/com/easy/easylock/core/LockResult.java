package com.easy.easylock.core;

import lombok.Data;

/**
 * 锁操作结果
 */
@Data
public class LockResult {
    
    /**
     * 锁对象
     */
    private Lock lock;
    
    /**
     * 是否成功获取锁
     */
    private boolean acquired;
} 