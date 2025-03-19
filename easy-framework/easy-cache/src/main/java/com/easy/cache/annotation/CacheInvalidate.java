package com.easy.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存失效注解
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheInvalidate {
    /**
     * 缓存名称
     */
    String name() default "";

    /**
     * 缓存key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 缓存条件，支持SpEL
     */
    String condition() default "";

    /**
     * 是否清空所有缓存
     */
    boolean allEntries() default false;
}