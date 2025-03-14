package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 启用缓存穿透保护
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePenetrationProtect {
    
    /**
     * 布隆过滤器的预计元素数量
     */
    int expectedSize() default 1000000;
    
    /**
     * 布隆过滤器的假阳性概率
     */
    double fpp() default 0.01;
} 