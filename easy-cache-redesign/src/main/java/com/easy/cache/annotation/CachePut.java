package com.easy.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存更新注解，标记在方法上表示该方法的返回值将更新缓存
 * 与Cacheable不同的是，CachePut标记的方法总是会被执行，其结果被用于更新缓存
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePut {

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
     * 条件表达式，满足条件时才更新缓存，支持SpEL表达式
     */
    String condition() default "";

    /**
     * 是否使用多级缓存
     */
    boolean useMultiLevel() default false;
}