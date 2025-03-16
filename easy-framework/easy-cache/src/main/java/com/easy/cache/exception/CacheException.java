package com.easy.cache.exception;

/**
 * 缓存操作异常
 */
public class CacheException extends RuntimeException {

    /**
     * 创建缓存异常
     * 
     * @param message 异常消息
     */
    public CacheException(String message) {
        super(message);
    }

    /**
     * 创建缓存异常
     * 
     * @param message 异常消息
     * @param cause   异常原因
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }
}