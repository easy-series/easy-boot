package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存清除注解，标记在方法上表示该方法执行时会清除指定的缓存
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvict {

    /**
     * 缓存名称
     */
    String cacheName();

    /**
     * 缓存键，支持SpEL表达式，默认使用方法参数作为键
     */
    String key() default "";

    /**
     * 键生成器的Bean名称，用于生成缓存键
     */
    String keyGenerator() default "";

    /**
     * 是否清除所有缓存内容
     */
    boolean allEntries() default false;

    /**
     * 是否在方法执行前清除缓存，默认在方法执行后清除
     */
    boolean beforeInvocation() default false;

    /**
     * 条件表达式，满足条件时才清除缓存，支持SpEL表达式
     */
    String condition() default "";
} 