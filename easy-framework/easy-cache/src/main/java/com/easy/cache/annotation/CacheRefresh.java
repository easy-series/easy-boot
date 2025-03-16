package com.easy.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 缓存刷新注解，配置缓存自动刷新
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheRefresh {

    /**
     * 刷新间隔
     */
    long refresh() default 0;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 最后访问后停止刷新的时间（秒）
     */
    long stopRefreshAfterLastAccess() default 0;
}