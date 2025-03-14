package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存数据注解，标记在方法上表示该方法的结果将被缓存
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {

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
     * 缓存过期时间（秒），0表示使用默认值
     */
    int expireTime() default 0;

    /**
     * 条件表达式，满足条件时才缓存，支持SpEL表达式
     */
    String condition() default "";

    /**
     * 是否使用多级缓存
     */
    boolean useMultiLevel() default false;
} 