package com.easy.cache.core;

/**
 * 结果代码定义
 */
public class ResultCode {
    
    /**
     * 操作成功
     */
    public static final int SUCCESS = 200;
    
    /**
     * 未找到
     */
    public static final int NOT_FOUND = 404;
    
    /**
     * 锁获取失败
     */
    public static final int FAIL_LOCK = 4001;
    
    /**
     * 超时
     */
    public static final int TIMEOUT = 4002;
    
    /**
     * 系统错误
     */
    public static final int SYSTEM_ERROR = 5000;
    
    /**
     * 连接失败
     */
    public static final int FAIL_CONNECTION = 5001;
    
    /**
     * 序列化错误
     */
    public static final int FAIL_SERIALIZATION = 5002;
    
    /**
     * 反序列化错误
     */
    public static final int FAIL_DESERIALIZATION = 5003;
} 