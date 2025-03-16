package com.easy.lock.aop;

import com.easy.lock.core.LockInfo;
import com.easy.lock.annotation.EasyLock;
import com.easy.lock.exception.LockException;
import com.easy.lock.template.LockTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 锁注解拦截器
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class LockInterceptor {

    private final LockTemplate lockTemplate;
    
    /**
     * SpEL表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();
    
    /**
     * 方法参数名发现器
     */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    
    /**
     * 表达式缓存
     */
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>(64);

    /**
     * 环绕通知处理锁注解
     */
    @Around("@annotation(com.easy.lock.annotation.EasyLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        EasyLock lockAnnotation = method.getAnnotation(EasyLock.class);
        
        // 获取锁键
        String lockKey = generateLockKey(joinPoint, lockAnnotation);
        log.debug("开始获取分布式锁，方法：{}，锁键：{}", method.getName(), lockKey);
        
        // 转换为毫秒
        long expireTime = lockAnnotation.timeUnit().toMillis(lockAnnotation.expire());
        
        // 尝试获取锁
        LockInfo lockInfo = null;
        try {
            lockInfo = lockTemplate.tryLock(lockKey, expireTime, lockAnnotation.retryCount(), lockAnnotation.retryInterval());
            
            if (lockInfo == null) {
                log.warn("获取分布式锁失败，方法：{}，锁键：{}", method.getName(), lockKey);
                // 处理加锁失败情况
                if (lockAnnotation.failStrategy() == EasyLock.FailStrategy.EXCEPTION) {
                    throw new LockException("获取锁失败，key = " + lockKey);
                } else {
                    // 忽略锁，继续执行
                    log.warn("忽略锁失败，继续执行，方法：{}，锁键：{}", method.getName(), lockKey);
                    return joinPoint.proceed();
                }
            }
            
            // 执行原方法
            log.debug("获取分布式锁成功，方法：{}，锁键：{}", method.getName(), lockKey);
            return joinPoint.proceed();
        } finally {
            // 释放锁
            if (lockInfo != null) {
                boolean released = lockTemplate.unlock(lockInfo);
                log.debug("释放分布式锁{}，方法：{}，锁键：{}", released ? "成功" : "失败", method.getName(), lockKey);
            }
        }
    }

    /**
     * 生成锁键
     *
     * @param joinPoint 连接点
     * @param lockAnnotation 锁注解
     * @return 完整的锁键
     */
    private String generateLockKey(ProceedingJoinPoint joinPoint, EasyLock lockAnnotation) {
        String prefix = lockAnnotation.prefix();
        String spelKey = lockAnnotation.key();
        String key;
        
        if (spelKey.contains("#")) {
            // 包含SpEL表达式，需要解析
            key = parseSpELKey(spelKey, joinPoint);
        } else {
            // 直接使用键值
            key = spelKey;
        }
        
        // 拼接前缀与键
        return prefix.isEmpty() ? key : (prefix + ":" + key);
    }

    /**
     * 解析SpEL表达式
     *
     * @param spelKey SpEL表达式
     * @param joinPoint 连接点
     * @return 解析后的键
     */
    private String parseSpELKey(String spelKey, ProceedingJoinPoint joinPoint) {
        try {
            // 从缓存获取表达式
            Expression expression = expressionCache.get(spelKey);
            if (expression == null) {
                expression = parser.parseExpression(spelKey);
                expressionCache.put(spelKey, expression);
            }
            
            // 创建评估上下文
            EvaluationContext context = createEvaluationContext(joinPoint);
            
            // 解析表达式
            Object value = expression.getValue(context);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            log.error("解析SpEL表达式失败：{}", spelKey, e);
            return spelKey;
        }
    }

    /**
     * 创建表达式评估上下文
     *
     * @param joinPoint 连接点
     * @return 评估上下文
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 获取方法签名
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        
        // 获取方法参数名
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        
        // 获取参数值
        Object[] args = joinPoint.getArgs();
        
        // 将参数添加到上下文
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        
        // 添加其他通用变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("method", method.getName());
        variables.put("class", method.getDeclaringClass().getSimpleName());
        variables.put("target", joinPoint.getTarget());
        context.setVariables(variables);
        
        return context;
    }
}