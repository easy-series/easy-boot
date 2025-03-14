package com.easy.cache.key;

import java.lang.reflect.Method;

/**
 * 基于SpEL表达式的键生成器
 */
public class SpelKeyGenerator implements KeyGenerator {

    /**
     * SpEL表达式解析器
     */
    private final SpELParser spELParser;

    /**
     * SpEL表达式
     */
    private final String expression;

    /**
     * 构造函数
     *
     * @param expression SpEL表达式
     */
    public SpelKeyGenerator(String expression) {
        this.spELParser = new SpELParser();
        this.expression = expression;
    }

    @Override
    public String generate(Object target, Method method, Object... params) {
        Object result = spELParser.parse(expression, method, params, target, null);
        return result != null ? result.toString() : "null";
    }

    /**
     * 解析SpEL表达式，支持方法执行结果
     *
     * @param target 目标对象
     * @param method 方法
     * @param params 方法参数
     * @param result 方法执行结果
     * @return 解析后的键
     */
    public String generateWithResult(Object target, Method method, Object[] params, Object result) {
        Object parsedKey = spELParser.parse(expression, method, params, target, result);
        return parsedKey != null ? parsedKey.toString() : "null";
    }
} 