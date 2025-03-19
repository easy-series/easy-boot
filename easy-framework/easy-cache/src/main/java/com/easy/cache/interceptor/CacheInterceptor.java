package com.easy.cache.interceptor;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.easy.cache.annotation.CacheInvalidate;
import com.easy.cache.annotation.CacheUpdate;
import com.easy.cache.annotation.Cached;
import com.easy.cache.key.KeyGenerator;
import com.easy.cache.key.SpELKeyGenerator;
import com.easy.cache.service.CacheService;

/**
 * 缓存拦截器
 * 负责处理缓存注解的方法调用
 */
@Component
public class CacheInterceptor implements MethodInterceptor {

    @Autowired
    private CacheService cacheService;

    private final KeyGenerator keyGenerator = new SpELKeyGenerator();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object target = invocation.getThis();
        Object[] arguments = invocation.getArguments();

        // 处理@Cached注解
        Cached cachedAnnotation = AnnotationUtils.findAnnotation(method, Cached.class);
        if (cachedAnnotation != null) {
            return handleCachedAnnotation(cachedAnnotation, invocation, target, method, arguments);
        }

        // 处理@CacheUpdate注解
        CacheUpdate cacheUpdateAnnotation = AnnotationUtils.findAnnotation(method, CacheUpdate.class);
        if (cacheUpdateAnnotation != null) {
            return handleCacheUpdateAnnotation(cacheUpdateAnnotation, invocation, target, method, arguments);
        }

        // 处理@CacheInvalidate注解
        CacheInvalidate cacheInvalidateAnnotation = AnnotationUtils.findAnnotation(method, CacheInvalidate.class);
        if (cacheInvalidateAnnotation != null) {
            return handleCacheInvalidateAnnotation(cacheInvalidateAnnotation, invocation, target, method, arguments);
        }

        // 无注解，直接调用原方法
        return invocation.proceed();
    }

    /**
     * 处理@Cached注解
     */
    private Object handleCachedAnnotation(Cached annotation, MethodInvocation invocation,
            Object target, Method method, Object[] arguments) throws Throwable {
        String cacheName = annotation.name();
        String keyExpression = annotation.key();

        Object cacheKey = generateKey(keyExpression, target, method, arguments);

        // 尝试从缓存获取
        Object cachedValue = cacheService.get(cacheName, cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 缓存未命中，调用原方法
        Object result = invocation.proceed();

        // 缓存结果
        if (result != null) {
            cacheService.put(cacheName, cacheKey, result);
        }

        return result;
    }

    /**
     * 处理@CacheUpdate注解
     */
    private Object handleCacheUpdateAnnotation(CacheUpdate annotation, MethodInvocation invocation,
            Object target, Method method, Object[] arguments) throws Throwable {
        // 先执行方法
        Object result = invocation.proceed();

        // 更新缓存
        String cacheName = annotation.name();
        String keyExpression = annotation.key();

        Object cacheKey = generateKey(keyExpression, target, method, arguments);

        if (result != null) {
            cacheService.put(cacheName, cacheKey, result);
        }

        return result;
    }

    /**
     * 处理@CacheInvalidate注解
     */
    private Object handleCacheInvalidateAnnotation(CacheInvalidate annotation, MethodInvocation invocation,
            Object target, Method method, Object[] arguments) throws Throwable {
        // 先执行方法
        Object result = invocation.proceed();

        // 清除缓存
        String cacheName = annotation.name();
        String keyExpression = annotation.key();
        boolean allEntries = annotation.allEntries();

        if (allEntries) {
            // 清除所有条目
            cacheService.clear(cacheName);
        } else {
            // 清除指定键
            Object cacheKey = generateKey(keyExpression, target, method, arguments);
            cacheService.remove(cacheName, cacheKey);
        }

        return result;
    }

    /**
     * 生成缓存键
     */
    private Object generateKey(String keyExpression, Object target, Method method, Object[] arguments) {
        if (keyExpression.isEmpty()) {
            return keyGenerator.generate(target, method, arguments);
        } else {
            return ((SpELKeyGenerator) keyGenerator).generateKey(keyExpression, method, arguments);
        }
    }
}