package com.easy.cache.aop;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 默认缓存键生成器
 */
public class DefaultCacheKeyGenerator implements CacheKeyGenerator {

    /**
     * SpEL表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 参数分隔符
     */
    private static final String PARAM_SEPARATOR = ":";

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder sb = new StringBuilder();
        // 添加类名
        sb.append(target.getClass().getSimpleName());
        sb.append(PARAM_SEPARATOR);
        // 添加方法名
        sb.append(method.getName());

        // 添加参数
        if (params != null && params.length > 0) {
            for (Object param : params) {
                sb.append(PARAM_SEPARATOR);
                if (param == null) {
                    sb.append("NULL");
                } else {
                    sb.append(param.toString());
                }
            }
        }

        return sb.toString();
    }

    @Override
    public Object parseExpression(String key, EvaluationContext context) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        // 解析SpEL表达式
        return parser.parseExpression(key).getValue(context);
    }

    /**
     * 创建SpEL表达式上下文
     *
     * @param method 方法
     * @param args   方法参数
     * @param target 目标对象
     * @param result 方法执行结果
     * @return 表达式上下文
     */
    public static EvaluationContext createEvaluationContext(Method method, Object[] args, Object target,
            Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 将方法参数添加到上下文
        if (args != null) {
            String[] parameterNames = resolveParameterNames(method);
            for (int i = 0; i < args.length; i++) {
                // 同时支持 #paramName 和 #p0, #p1, ... 形式的表达式
                context.setVariable("p" + i, args[i]);
                if (parameterNames != null && i < parameterNames.length) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            // 添加整个参数数组
            context.setVariable("args", args);
        }

        // 添加目标对象
        context.setVariable("target", target);

        // 添加方法执行结果（用于CachePut注解）
        if (result != null) {
            context.setVariable("result", result);
        }

        // 添加方法
        context.setVariable("method", method);

        return context;
    }

    /**
     * 解析方法参数名称
     * 注：在实际应用中可使用更高级的工具解析方法参数名称
     *
     * @param method 方法
     * @return 参数名称数组
     */
    private static String[] resolveParameterNames(Method method) {
        // 简单实现，仅使用参数索引作为名称
        return Arrays.stream(method.getParameters())
                .map(parameter -> parameter.getName())
                .toArray(String[]::new);
    }
}