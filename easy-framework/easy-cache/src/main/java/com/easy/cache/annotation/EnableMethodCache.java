package com.easy.cache.annotation;

import com.easy.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用方法缓存注解
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ CacheAutoConfiguration.class })
public @interface EnableMethodCache {

    /**
     * 扫描包路径
     */
    String[] basePackages() default {};

    /**
     * 是否启用本地缓存
     */
    boolean enableLocalCache() default true;

    /**
     * 是否启用远程缓存
     */
    boolean enableRemoteCache() default true;

    /**
     * 是否启用缓存同步
     */
    boolean enableCacheSync() default false;

    /**
     * 是否启用自动刷新
     */
    boolean enableAutoRefresh() default false;

    /**
     * 是否启用穿透保护
     */
    boolean enablePenetrationProtect() default false;
}