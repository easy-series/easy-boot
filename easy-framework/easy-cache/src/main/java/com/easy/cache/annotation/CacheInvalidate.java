package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存失效注解，用于标记需要使缓存失效的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheInvalidate {

    /**
     * 缓存名称，默认为空，会使用类名.方法名作为缓存名称
     */
    String name() default "";

    /**
     * 缓存键，支持SpEL表达式，例如：#userId 或 #user.id
     */
    String key();

    /**
     * 是否清空整个缓存，默认为false
     */
    boolean allEntries() default false;
} 