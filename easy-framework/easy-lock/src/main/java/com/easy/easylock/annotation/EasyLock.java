package com.easy.easylock.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * 用于标记需要加锁的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EasyLock {

    /**
     * 锁的名称，默认为空，将使用 类名.方法名
     */
    String name() default "";

    /**
     * 锁的key，支持SpEL表达式
     * 可以从方法参数中提取值作为锁的key
     */
    String key() default "";

    /**
     * 获取锁等待时间，默认3秒
     */
    long waitTime() default 3000L;

    /**
     * 锁的持有时间(过期时间)，默认30秒
     */
    long leaseTime() default 30000L;

    /**
     * 时间单位，默认毫秒
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 获取锁失败时，是否抛出异常
     */
    boolean throwException() default true;

    /**
     * 获取锁失败时的错误消息
     */
    String failMessage() default "获取分布式锁失败";
}