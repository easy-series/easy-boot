package com.easy.lock.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EasyLock {

    /**
     * 锁的键
     * 支持Spring EL表达式
     */
    String key();

    /**
     * 锁的前缀
     */
    String prefix() default "";

    /**
     * 锁的过期时间，默认30秒
     */
    long expire() default 30000L;

    /**
     * 锁的过期时间单位，默认毫秒
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 获取锁失败时的重试次数
     */
    int retryCount() default 3;

    /**
     * 获取锁失败时的重试间隔（毫秒）
     */
    long retryInterval() default 100L;

    /**
     * 获取锁失败时的处理方式
     */
    FailStrategy failStrategy() default FailStrategy.EXCEPTION;

    /**
     * 锁失败策略枚举
     */
    enum FailStrategy {
        /**
         * 抛出异常
         */
        EXCEPTION,
        
        /**
         * 忽略并继续执行
         */
        IGNORE
    }
}