package com.easy.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 配置缓存自动刷新
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheRefresh {
    
    /**
     * 刷新周期，单位秒
     */
    long refresh() default 1800;
    
    /**
     * 刷新周期的时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 是否随机延迟刷新，防止缓存雪崩
     */
    boolean randomDelay() default true;
    
    /**
     * 最大随机延迟时间(秒)
     */
    int maxRandomDelay() default 300;
} 