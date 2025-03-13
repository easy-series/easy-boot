package com.easy.cache.aop;

import com.easy.cache.annotation.*;
import com.easy.cache.core.CacheManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 缓存切面，用于拦截缓存注解
 */
@Aspect
public class CacheAspect {

    private final CacheInterceptor cacheInterceptor;

    public CacheAspect(CacheManager cacheManager) {
        this.cacheInterceptor = new CacheInterceptor(cacheManager);
    }

    /**
     * 拦截@Cached注解
     */
    @Around("@annotation(com.easy.cache.annotation.Cached)")
    public Object aroundCached(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        // 获取缓存注解
        Cached cached = method.getAnnotation(Cached.class);
        CacheRefresh refresh = method.getAnnotation(CacheRefresh.class);
        CachePenetrationProtect protect = method.getAnnotation(CachePenetrationProtect.class);

        // 处理缓存
        return cacheInterceptor.handleCached(method, args, cached, refresh, protect, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 拦截@CacheUpdate注解
     */
    @Around("@annotation(com.easy.cache.annotation.CacheUpdate)")
    public Object aroundCacheUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        // 获取缓存更新注解
        CacheUpdate update = method.getAnnotation(CacheUpdate.class);

        // 执行原方法
        Object result = joinPoint.proceed();

        // 处理缓存更新
        cacheInterceptor.handleCacheUpdate(method, args, update, result);

        return result;
    }

    /**
     * 拦截@CacheInvalidate注解
     */
    @Around("@annotation(com.easy.cache.annotation.CacheInvalidate)")
    public Object aroundCacheInvalidate(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        // 获取缓存失效注解
        CacheInvalidate invalidate = method.getAnnotation(CacheInvalidate.class);

        // 执行原方法
        Object result = joinPoint.proceed();

        // 处理缓存失效
        cacheInterceptor.handleCacheInvalidate(method, args, invalidate, result);

        return result;
    }
} 