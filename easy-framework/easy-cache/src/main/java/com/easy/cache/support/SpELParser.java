package com.easy.cache.support;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL表达式解析器，用于解析缓存键和值
 */
public class SpELParser {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    /**
     * 解析SpEL表达式
     * 
     * @param expression SpEL表达式
     * @param method     方法
     * @param args       方法参数
     * @param result     方法返回值
     * @param resultType 返回值类型
     * @return 解析结果
     */
    public static <T> T parse(String expression, Method method, Object[] args, Object result, Class<T> resultType) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }

        // 创建评估上下文
        EvaluationContext context = createContext(method, args, result);

        // 解析表达式
        Expression expr = PARSER.parseExpression(expression);
        return expr.getValue(context, resultType);
    }

    /**
     * 创建评估上下文
     * 
     * @param method 方法
     * @param args   方法参数
     * @param result 方法返回值
     * @return 评估上下文
     */
    private static EvaluationContext createContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 添加方法参数
        if (args != null && args.length > 0) {
            // 添加参数数组
            context.setVariable("args", args);

            // 添加参数名称
            String[] parameterNames = getParameterNames(method);
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // 添加p0, p1, ... 形式的参数
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
            }
        }

        // 添加方法返回值
        if (result != null) {
            context.setVariable("result", result);
        }

        return context;
    }

    /**
     * 获取方法参数名称
     * 
     * @param method 方法
     * @return 参数名称数组
     */
    private static String[] getParameterNames(Method method) {
        if (method == null) {
            return null;
        }

        // 获取参数名称（需要JDK 8及以上，并且编译时加上-parameters参数）
        return java.util.Arrays.stream(method.getParameters())
                .map(java.lang.reflect.Parameter::getName)
                .toArray(String[]::new);
    }

    /**
     * 生成默认的缓存键
     * 
     * @param method 方法
     * @param args   方法参数
     * @return 缓存键
     */
    public static String generateDefaultKey(Method method, Object[] args) {
        StringBuilder sb = new StringBuilder();

        // 添加方法签名
        sb.append(method.getDeclaringClass().getName())
                .append(".")
                .append(method.getName())
                .append("(");

        // 添加参数
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(args[i] == null ? "null" : args[i].toString());
            }
        }

        sb.append(")");
        return sb.toString();
    }
}