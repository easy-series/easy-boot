package com.easy.cache.support;

import java.lang.reflect.Method;

/**
 * 缓存键生成器接口
 */
public interface KeyGenerator {
    
    /**
     * 生成缓存键
     * 
     * @param target 目标对象
     * @param method 方法
     * @param params 方法参数
     * @return 缓存键
     */
    Object generate(Object target, Method method, Object... params);
} 