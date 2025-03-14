package com.easy.easylock.aop;

import com.easy.easylock.annotation.EasyLock;
import com.easy.easylock.core.Lock;
import com.easy.easylock.core.LockManager;
import com.easy.easylock.core.LockResult;
import com.easy.easylock.support.SpELParser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 锁拦截器，处理带有@EasyLock注解的方法
 */
@Aspect
@Slf4j
public class LockInterceptor {

    private final LockManager lockManager;

    /**
     * 构造函数
     *
     * @param lockManager 锁管理器
     */
    public LockInterceptor(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    /**
     * 环绕通知，处理带有@EasyLock注解的方法
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(com.easy.easylock.annotation.EasyLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取注解
        EasyLock lockAnnotation = method.getAnnotation(EasyLock.class);
        
        // 创建锁对象
        Lock lock = createLock(lockAnnotation, method, joinPoint.getArgs(), joinPoint.getTarget());
        
        // 尝试获取锁
        LockResult lockResult = lockManager.acquire(lock);
        
        try {
            // 如果获取锁成功或者不需要锁时，执行原方法
            if (lockResult.isAcquired()) {
                log.debug("获取锁成功 - name: {}", lock.getFullName());
                return joinPoint.proceed();
            } else {
                // 获取锁失败但不抛出异常时，返回null
                log.warn("获取锁失败 - name: {}", lock.getFullName());
                return null;
            }
        } finally {
            // 释放锁
            if (lockResult.isAcquired()) {
                lockManager.release(lockResult);
            }
        }
    }

    /**
     * 根据注解创建锁对象
     *
     * @param lockAnnotation 锁注解
     * @param method         方法
     * @param args           方法参数
     * @param target         目标对象
     * @return 锁对象
     */
    private Lock createLock(EasyLock lockAnnotation, Method method, Object[] args, Object target) {
        // 创建锁对象
        Lock lock = new Lock();
        
        // 设置锁名称，如果注解没有指定则使用"类名.方法名"
        String name = lockAnnotation.name();
        if (!StringUtils.hasText(name)) {
            name = method.getDeclaringClass().getName() + "." + method.getName();
        }
        lock.setName(name);
        
        // 解析锁的key
        String keyExpression = lockAnnotation.key();
        if (StringUtils.hasText(keyExpression)) {
            // 解析SpEL表达式
            String key = SpELParser.parseExpression(keyExpression, method, args, target);
            lock.setKey(key);
        } else if (args != null && args.length > 0) {
            // 如果没有指定key且有参数，则使用第一个参数的字符串值作为key
            lock.setKey(String.valueOf(args[0].hashCode()));
        }
        
        // 设置其他属性
        lock.setWaitTime(lockAnnotation.waitTime())
            .setLeaseTime(lockAnnotation.leaseTime())
            .setTimeUnit(lockAnnotation.timeUnit())
            .setThrowException(lockAnnotation.throwException())
            .setFailMessage(lockAnnotation.failMessage());
        
        return lock;
    }
}