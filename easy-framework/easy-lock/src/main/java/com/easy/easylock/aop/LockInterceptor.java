package com.easy.easylock.aop;

import com.easy.easylock.annotation.EasyLock;
import com.easy.easylock.core.Lock;
import com.easy.easylock.core.LockExecutor;
import com.easy.easylock.exception.LockException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 分布式锁切面
 */
@Slf4j
@Aspect
public class LockInterceptor {

    private final LockExecutor lockExecutor;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public LockInterceptor(LockExecutor lockExecutor) {
        this.lockExecutor = lockExecutor;
    }

    @Around("@annotation(com.easy.easylock.annotation.EasyLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取锁注解
        EasyLock easyLock = method.getAnnotation(EasyLock.class);
        if (easyLock == null) {
            return joinPoint.proceed();
        }

        // 获取锁名称，默认使用类名.方法名
        String name = easyLock.name();
        if (name.isEmpty()) {
            name = method.getDeclaringClass().getName() + "." + method.getName();
        }

        // 获取锁的key
        String key = easyLock.key();
        if (!key.isEmpty()) {
            // 支持SpEL表达式解析
            key = parseSpEL(key, method, joinPoint.getArgs());
        } else {
            // 默认使用方法参数的哈希码作为key
            key = String.valueOf(argsToString(joinPoint.getArgs()).hashCode());
        }

        // 创建锁对象
        Lock lock = new Lock()
                .setName(name)
                .setKey(key)
                .setValue(UUID.randomUUID().toString())
                .setWaitTime(easyLock.waitTime())
                .setLeaseTime(easyLock.leaseTime())
                .setTimeUnit(easyLock.timeUnit())
                .setThrowException(easyLock.throwException())
                .setFailMessage(easyLock.failMessage());

        boolean locked = false;
        try {
            // 尝试获取锁
            locked = lockExecutor.tryLock(lock);

            if (!locked) {
                // 获取锁失败
                if (easyLock.throwException()) {
                    throw new LockException(easyLock.failMessage());
                }
                log.warn("获取分布式锁失败: {}", lock.getFullName());
                return null;
            }

            // 获取锁成功，执行目标方法
            return joinPoint.proceed();
        } finally {
            // 释放锁
            if (locked) {
                boolean released = lockExecutor.releaseLock(lock);
                if (!released) {
                    log.warn("释放分布式锁失败: {}", lock.getFullName());
                }
            }
        }
    }

    /**
     * 解析SpEL表达式
     */
    private String parseSpEL(String spEl, Method method, Object[] args) {
        // 创建计算上下文
        EvaluationContext context = new MethodBasedEvaluationContext(null, method, args, parameterNameDiscoverer);

        // 解析表达式
        Object value = expressionParser.parseExpression(spEl).getValue(context);
        return value == null ? "" : value.toString();
    }

    /**
     * 将参数转换为字符串
     */
    private String argsToString(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            builder.append(arg == null ? "null" : arg.toString());
        }
        return builder.toString();
    }
}