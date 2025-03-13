package com.easy.cache.annotation;

import com.easy.cache.core.CacheType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存注解，用于标记需要缓存的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cached {

    /**
     * 缓存名称，默认为空，会使用类名.方法名作为缓存名称
     */
    String name() default "";

    /**
     * 缓存键，支持SpEL表达式，例如：#userId 或 #user.id
     * 如果不指定，则使用所有参数生成缓存键
     */
    String key() default "";

    /**
     * 过期时间，单位由timeUnit指定，默认为0（永不过期）
     */
    long expire() default 0;

    /**
     * 过期时间单位，默认为秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 缓存类型，默认为本地缓存
     */
    CacheType cacheType() default CacheType.LOCAL;

    /**
     * 是否缓存空值，默认为true
     */
    boolean cacheNull() default true;

    /**
     * 是否启用写透模式，默认为true
     */
    boolean writeThrough() default true;

    /**
     * 是否启用异步写入，默认为false
     */
    boolean asyncWrite() default false;

    /**
     * 是否同步本地缓存，默认为true
     */
    boolean syncLocal() default true;

    /**
     * 本地缓存大小限制，默认为10000
     */
    int localLimit() default 10000;
}