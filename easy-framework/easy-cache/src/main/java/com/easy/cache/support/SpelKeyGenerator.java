package com.easy.cache.support;

import java.lang.reflect.Method;

/**
 * 基于SpEL表达式的缓存键生成器
 */
public class SpelKeyGenerator implements KeyGenerator {

    private final String expression;

    /**
     * 创建默认的SpEL键生成器
     */
    public SpelKeyGenerator() {
        this("#{methodName}:#{#p0}");
    }

    /**
     * 创建指定表达式的SpEL键生成器
     * 
     * @param expression SpEL表达式
     */
    public SpelKeyGenerator(String expression) {
        this.expression = expression;
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return SpelExpressionParser.parseExpression(expression, method, params, target);
    }
} 