package com.easy.id.exception;

/**
 * ID生成器异常
 */
public class IdGeneratorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造方法
     *
     * @param message 错误消息
     */
    public IdGeneratorException(String message) {
        super(message);
    }

    /**
     * 构造方法
     *
     * @param message 错误消息
     * @param cause   错误原因
     */
    public IdGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
} 