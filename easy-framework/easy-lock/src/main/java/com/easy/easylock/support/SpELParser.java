package com.easy.easylock.support;

import java.lang.reflect.Method;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

/**
 * SpEL表达式解析器，用于解析锁的key表达式
 */
public class SpELParser {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    /**
     * 解析SpEL表达式
     *
     * @param expressionString 表达式字符串
     * @param method           方法
     * @param args             方法参数
     * @param target           目标对象
     * @param returnType       返回类型
     * @param <T>              返回类型泛型
     * @return 解析结果
     */
    public static <T> T parseExpression(String expressionString, Method method, Object[] args, Object target,
            Class<T> returnType) {
        if (!StringUtils.hasText(expressionString)) {
            return null;
        }

        // 解析表达式
        Expression expression = PARSER.parseExpression(expressionString);

        // 创建表达式计算上下文
        EvaluationContext context = new MethodBasedEvaluationContext(target, method, args, NAME_DISCOVERER);

        // 计算表达式的值
        return expression.getValue(context, returnType);
    }

    /**
     * 解析SpEL表达式为字符串
     *
     * @param expressionString 表达式字符串
     * @param method           方法
     * @param args             方法参数
     * @param target           目标对象
     * @return 解析结果字符串
     */
    public static String parseExpression(String expressionString, Method method, Object[] args, Object target) {
        Object value = parseExpression(expressionString, method, args, target, Object.class);
        return value != null ? value.toString() : null;
    }
}