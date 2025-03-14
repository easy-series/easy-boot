package com.easy.cache.aop;

import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;

/**
 * 缓存键生成器接口
 */
public interface CacheKeyGenerator {

    /**
     * 生成缓存键
     *
     * @param target 目标对象
     * @param method 方法
     * @param params 方法参数
     * @return 缓存键
     */
    Object generate(Object target, Method method, Object... params);

    /**
     * 解析SpEL表达式
     *
     * @param key     SpEL表达式
     * @param context 表达式上下文
     * @return 解析后的值
     */
    Object parseExpression(String key, EvaluationContext context);
}