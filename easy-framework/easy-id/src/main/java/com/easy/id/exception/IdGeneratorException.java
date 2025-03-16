package com.easy.id.exception;

/**
 * ID生成器异常类
 *
 * @author 芋道源码
 */
public class IdGeneratorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdGeneratorException(String message) {
        super(message);
    }

    public IdGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdGeneratorException(Throwable cause) {
        super(cause);
    }
} 