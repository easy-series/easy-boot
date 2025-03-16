package com.easy.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存更新注解，标记需要更新缓存的方法
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheUpdate {

    /**
     * 缓存名称
     */
    String name() default "";

    /**
     * 缓存键，支持SpEL表达式
     */
    String key() default "";

    /**
     * 缓存值，支持SpEL表达式
     */
    String value() default "";
}