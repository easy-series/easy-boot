package com.easy.cache.annotation;

import com.easy.cache.config.EasyCacheAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用缓存功能的注解
 * 通常标注在配置类上
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EasyCacheAutoConfiguration.class)
public @interface EnableCaching {
    
    /**
     * 是否启用本地缓存
     */
    boolean enableLocalCache() default true;
    
    /**
     * 是否启用远程缓存(Redis)
     */
    boolean enableRemoteCache() default false;
    
    /**
     * 是否启用缓存同步
     */
    boolean enableCacheSync() default false;
    
    /**
     * 是否启用自动刷新
     */
    boolean enableAutoRefresh() default false;
    
    /**
     * 是否启用缓存穿透保护
     */
    boolean enablePenetrationProtect() default false;
} 