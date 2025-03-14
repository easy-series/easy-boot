package com.easy.cache.key;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * SpEL表达式解析器，用于解析缓存键表达式
 */
public class SpELParser {

    /**
     * SpEL表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 参数名发现器
     */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 解析方法参数中的SpEL表达式
     *
     * @param key SpEL表达式
     * @param method 方法
     * @param args 方法参数
     * @param target 目标对象
     * @param result 方法执行结果
     * @return 解析后的值
     */
    public Object parse(String key, Method method, Object[] args, Object target, Object result) {
        if (!StringUtils.hasText(key)) {
            return null;
        }

        // 如果不是SpEL表达式，直接返回
        if (!isSpELExpression(key)) {
            return key;
        }

        // 创建计算上下文
        EvaluationContext context = createEvaluationContext(method, args, target, result);

        // 解析表达式
        return parser.parseExpression(key).getValue(context);
    }

    /**
     * 判断是否为SpEL表达式
     *
     * @param key 键
     * @return 是否为SpEL表达式
     */
    private boolean isSpELExpression(String key) {
        return key.contains("#") || key.contains("'") || key.contains("\"") || key.contains("?:");
    }

    /**
     * 创建计算上下文
     *
     * @param method 方法
     * @param args 方法参数
     * @param target 目标对象
     * @param result 方法执行结果
     * @return 计算上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args, Object target, Object result) {
        StandardEvaluationContext context;

        if (method != null) {
            // 基于方法的上下文，支持获取方法参数名
            MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(
                    target, method, args, parameterNameDiscoverer);
            context = evaluationContext;
        } else {
            context = new StandardEvaluationContext();
        }

        // 设置参数以供 p0, p1, ... 访问
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
            }
        }

        // 设置 #result 变量
        if (result != null) {
            context.setVariable("result", result);
        }

        // 设置 #root 变量
        context.setVariable("root", args);

        return context;
    }
} 