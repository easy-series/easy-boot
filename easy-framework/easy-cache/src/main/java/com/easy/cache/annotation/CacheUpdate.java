package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存更新注解，用于标记需要更新缓存的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheUpdate {

    /**
     * 缓存名称，默认为空，会使用类名.方法名作为缓存名称
     */
    String name() default "";

    /**
     * 缓存键，支持SpEL表达式，例如：#userId 或 #user.id
     */
    String key();

    /**
     * 缓存值，支持SpEL表达式，例如：#result 或 #user
     */
    String value();
} 