package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 启用缓存注解，用于标记需要启用缓存功能的类或配置类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableCaching {

    /**
     * 是否启用本地缓存，默认为true
     */
    boolean enableLocalCache() default true;

    /**
     * 是否启用远程缓存，默认为false
     */
    boolean enableRemoteCache() default false;

    /**
     * 是否启用缓存同步，默认为false
     */
    boolean enableCacheSync() default false;

    /**
     * 是否启用自动刷新，默认为false
     */
    boolean enableAutoRefresh() default false;

    /**
     * 是否启用缓存穿透保护，默认为false
     */
    boolean enablePenetrationProtect() default false;
} 