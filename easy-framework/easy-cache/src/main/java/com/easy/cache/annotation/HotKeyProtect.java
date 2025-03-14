package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 启用热点键保护
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HotKeyProtect {
    
    /**
     * 热点Key检测阈值，默认1秒内请求5次认为是热点
     */
    int threshold() default 5;
    
    /**
     * 检测时间窗口(毫秒)
     */
    long windowInMillis() default 1000;
    
    /**
     * 热点Key的本地缓存时间(秒)
     */
    int localCacheSeconds() default 5;
} 