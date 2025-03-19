package com.easy.cache.key;

import java.lang.reflect.Method;

/**
 * 缓存键生成器接口
 */
public interface KeyGenerator {
    
    /**
     * 根据目标方法和参数生成缓存键
     *
     * @param target 目标对象
     * @param method 方法
     * @param params 方法参数
     * @return 生成的缓存键
     */
    Object generate(Object target, Method method, Object... params);
} 