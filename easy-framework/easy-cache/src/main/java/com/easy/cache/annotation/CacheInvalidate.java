package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 标记需要使缓存失效的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheInvalidate {
    
    /**
     * 缓存名称
     */
    String name();
    
    /**
     * 缓存键，支持SpEL表达式
     */
    String key();
    
    /**
     * 是否清除该缓存名称下的所有缓存
     */
    boolean clearAll() default false;
} 