package com.easy.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存自动刷新注解，用于标记需要自动刷新的缓存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheRefresh {

    /**
     * 刷新间隔，单位由timeUnit指定
     */
    long refresh();

    /**
     * 最后一次访问后停止刷新的时间，单位由timeUnit指定
     * 如果设置为0，则表示一直刷新
     */
    long stopRefreshAfterLastAccess() default 0;

    /**
     * 时间单位，默认为秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
} 