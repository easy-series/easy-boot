package com.easy.cache.util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * SpEL表达式键生成器
 */
public class SpELKeyGenerator {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 生成缓存键
     *
     * @param joinPoint     连接点
     * @param keyExpression 键表达式
     * @return 缓存键
     */
    public String generate(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (keyExpression == null || keyExpression.isEmpty()) {
            return generateDefaultKey(joinPoint);
        }
        return evaluateExpression(joinPoint, keyExpression, String.class);
    }
    
    /**
     * 生成缓存键
     *
     * @param invocation    方法调用
     * @param keyExpression 键表达式
     * @return 缓存键
     */
    public String generate(MethodInvocation invocation, String keyExpression) {
        if (keyExpression == null || keyExpression.isEmpty()) {
            return generateDefaultKey(invocation);
        }
        return evaluateExpression(invocation, keyExpression, String.class);
    }

    /**
     * 生成默认键
     *
     * @param joinPoint 连接点
     * @return 默认键
     */
    private String generateDefaultKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        StringBuilder sb = new StringBuilder();

        // 类名和方法名
        sb.append(method.getDeclaringClass().getName())
                .append(".")
                .append(method.getName());

        // 参数值
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            sb.append(":");
            for (Object arg : args) {
                sb.append(arg == null ? "null" : arg.toString()).append(",");
            }
            sb.deleteCharAt(sb.length() - 1); // 删除最后一个逗号
        }

        return sb.toString();
    }
    
    /**
     * 生成默认键
     *
     * @param invocation 方法调用
     * @return 默认键
     */
    private String generateDefaultKey(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        StringBuilder sb = new StringBuilder();

        // 类名和方法名
        sb.append(method.getDeclaringClass().getName())
                .append(".")
                .append(method.getName());

        // 参数值
        Object[] args = invocation.getArguments();
        if (args != null && args.length > 0) {
            sb.append(":");
            for (Object arg : args) {
                sb.append(arg == null ? "null" : arg.toString()).append(",");
            }
            sb.deleteCharAt(sb.length() - 1); // 删除最后一个逗号
        }

        return sb.toString();
    }

    /**
     * 评估条件表达式
     *
     * @param joinPoint           连接点
     * @param conditionExpression 条件表达式
     * @return 条件结果
     */
    public boolean condition(ProceedingJoinPoint joinPoint, String conditionExpression) {
        if (conditionExpression == null || conditionExpression.isEmpty()) {
            return true;
        }
        return evaluateExpression(joinPoint, conditionExpression, Boolean.class);
    }
    
    /**
     * 评估条件表达式
     *
     * @param invocation          方法调用
     * @param conditionExpression 条件表达式
     * @return 条件结果
     */
    public boolean condition(MethodInvocation invocation, String conditionExpression) {
        if (conditionExpression == null || conditionExpression.isEmpty()) {
            return true;
        }
        return evaluateExpression(invocation, conditionExpression, Boolean.class);
    }

    /**
     * 评估表达式
     *
     * @param joinPoint        连接点
     * @param expressionString 表达式字符串
     * @param resultType       结果类型
     * @param <T>              结果泛型
     * @return 表达式计算结果
     */
    private <T> T evaluateExpression(ProceedingJoinPoint joinPoint, String expressionString, Class<T> resultType) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = signature.getParameterNames();
        Parameter[] parameters = method.getParameters();

        EvaluationContext context = new StandardEvaluationContext();

        // 设置参数
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);

            // 支持 "#param.property" 形式
            if (args[i] != null) {
                context.setVariable("p" + i, args[i]);
            }
        }

        // 设置方法返回值（仅在 @CacheUpdate 等情况下可用）
        context.setVariable("result", null);
        context.setVariable("returnValue", null);

        // 设置目标对象
        context.setVariable("target", joinPoint.getTarget());

        // 解析表达式
        Expression expression = parser.parseExpression(expressionString);

        return expression.getValue(context, resultType);
    }
    
    /**
     * 评估表达式
     *
     * @param invocation       方法调用
     * @param expressionString 表达式字符串
     * @param resultType       结果类型
     * @param <T>              结果泛型
     * @return 表达式计算结果
     */
    private <T> T evaluateExpression(MethodInvocation invocation, String expressionString, Class<T> resultType) {
        Method method = invocation.getMethod();
        Object[] args = invocation.getArguments();
        Parameter[] parameters = method.getParameters();

        EvaluationContext context = new StandardEvaluationContext();

        // 设置参数
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            context.setVariable(paramName, args[i]);

            // 支持 "#param.property" 形式
            if (args[i] != null) {
                context.setVariable("p" + i, args[i]);
            }
        }

        // 设置方法返回值（仅在 @CacheUpdate 等情况下可用）
        context.setVariable("result", null);
        context.setVariable("returnValue", null);

        // 设置目标对象
        context.setVariable("target", invocation.getThis());

        // 解析表达式
        Expression expression = parser.parseExpression(expressionString);

        return expression.getValue(context, resultType);
    }
}