package com.easy.cache.annotation;

import com.easy.cache.core.CacheType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 标记需要缓存的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cached {
    
    /**
     * 缓存名称
     */
    String name();
    
    /**
     * 缓存键，支持SpEL表达式
     */
    String key();
    
    /**
     * 缓存过期时间，默认1小时
     */
    long expire() default 3600;
    
    /**
     * 过期时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 缓存类型
     */
    CacheType cacheType() default CacheType.LOCAL;
    
    /**
     * 是否缓存null值
     */
    boolean cacheNull() default false;
    
    /**
     * 自定义键生成器的bean名称
     */
    String keyGenerator() default "";
} 