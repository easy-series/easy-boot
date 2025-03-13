package com.easy.easylock.annotation;

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
     * 锁的名称
     * <p>
     * 如果不设置，则使用 {类名}.{方法名} 作为锁名
     */
    String name() default "";

    /**
     * 锁的key，支持Spring EL表达式
     * <p>
     * 如果不设置，则使用方法的参数作为key
     */
    String key() default "";

    /**
     * 获取锁的等待时间，默认等待3秒
     */
    long waitTime() default 3000L;

    /**
     * 锁的过期时间，默认30秒
     */
    long leaseTime() default 30000L;

    /**
     * 时间单位，默认为毫秒
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 获取锁失败时，是否抛出异常
     */
    boolean throwException() default true;
    
    /**
     * 自定义加锁失败提示信息
     */
    String failMessage() default "获取分布式锁失败";
} 