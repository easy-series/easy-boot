package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 标记需要更新缓存的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheUpdate {
    
    /**
     * 缓存名称
     */
    String name();
    
    /**
     * 缓存键，支持SpEL表达式
     */
    String key();
    
    /**
     * 缓存值，支持SpEL表达式
     */
    String value() default "#result";
} 