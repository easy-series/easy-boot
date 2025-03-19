package com.easy.cache.key;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 基于SpEL表达式的缓存键生成器
 */
public class SpELKeyGenerator implements KeyGenerator {

    private final ExpressionParser parser = new SpelExpressionParser();
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length == 0) {
            return generateKeyWithoutParams(target, method);
        }
        
        String key = target.getClass().getSimpleName() + "." + method.getName();
        return key + ":" + Arrays.stream(params)
                .map(param -> param == null ? "null" : param.toString())
                .collect(Collectors.joining(","));
    }
    
    /**
     * 根据SpEL表达式生成缓存键
     *
     * @param expression SpEL表达式
     * @param method 方法
     * @param args 方法参数
     * @return 生成的缓存键
     */
    public Object generateKey(String expression, Method method, Object... args) {
        if (expression == null || expression.isEmpty()) {
            return generate(method.getDeclaringClass(), method, args);
        }
        
        EvaluationContext context = createEvaluationContext(method, args);
        Expression parsedExpression = parser.parseExpression(expression);
        return parsedExpression.getValue(context);
    }
    
    /**
     * 创建表达式评估上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 添加方法参数到上下文
        Object[] parameterValues = args;
        String[] parameterNames = getParameterNames(method);
        
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (i < parameterValues.length) {
                    context.setVariable(parameterNames[i], parameterValues[i]);
                }
            }
        }
        
        for (int i = 0; i < parameterValues.length; i++) {
            context.setVariable("p" + i, parameterValues[i]);
        }
        
        context.setVariable("method", method);
        context.setVariable("args", parameterValues);
        context.setVariable("target", method.getDeclaringClass());
        
        return context;
    }
    
    /**
     * 获取方法参数名
     * 注意：这里简化处理，实际项目中可以通过反射、ASM或Spring的ParameterNameDiscoverer获取参数名
     */
    private String[] getParameterNames(Method method) {
        int parameterCount = method.getParameterCount();
        String[] parameterNames = new String[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            parameterNames[i] = "arg" + i;
        }
        return parameterNames;
    }
    
    /**
     * 无参数方法的键生成
     */
    private Object generateKeyWithoutParams(Object target, Method method) {
        return target.getClass().getSimpleName() + "." + method.getName();
    }
} 