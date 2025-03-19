package com.easy.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存注解
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cached {

    /**
     * 缓存名称
     */
    String name() default "";

    /**
     * 缓存key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 过期时间（秒）
     */
    long expire() default -1;

    /**
     * 是否本地缓存
     */
    boolean local() default true;

    /**
     * 最大容量
     */
    int maxSize() default 10000;

    /**
     * 是否开启统计
     */
    boolean enableStats() default true;
}