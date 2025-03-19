package com.easy.cache.spring;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.easy.cache.annotation.CacheInvalidate;
import com.easy.cache.annotation.CacheUpdate;
import com.easy.cache.annotation.Cached;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.util.SpELKeyGenerator;

/**
 * 缓存拦截器
 */
@Component
public class CacheInterceptor implements MethodInterceptor {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SpELKeyGenerator keyGenerator;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();

        // 处理@Cached注解
        Cached cached = method.getAnnotation(Cached.class);
        if (cached != null) {
            return handleCached(invocation, cached);
        }

        // 处理@CacheUpdate注解
        CacheUpdate cacheUpdate = method.getAnnotation(CacheUpdate.class);
        if (cacheUpdate != null) {
            return handleCacheUpdate(invocation, cacheUpdate);
        }

        // 处理@CacheInvalidate注解
        CacheInvalidate cacheInvalidate = method.getAnnotation(CacheInvalidate.class);
        if (cacheInvalidate != null) {
            return handleCacheInvalidate(invocation, cacheInvalidate);
        }

        return invocation.proceed();
    }

    private Object handleCached(MethodInvocation invocation, Cached cached) throws Throwable {
        String cacheName = cached.name();
        if (cacheName.isEmpty()) {
            Method method = invocation.getMethod();
            cacheName = method.getDeclaringClass().getName() + "." + method.getName();
        }

        // 生成缓存key
        String key = keyGenerator.generate(invocation, cached.key());

        // 获取缓存
        Cache<String, Object> cache = cacheManager.getCache(cacheName);

        // 尝试从缓存获取
        Object value = cache.get(key);
        if (value != null) {
            return value;
        }

        // 缓存未命中，执行原方法
        value = invocation.proceed();

        // 放入缓存
        if (value != null) {
            if (cached.expire() > 0) {
                cache.put(key, value, cached.expire());
            } else {
                cache.put(key, value);
            }
        }

        return value;
    }

    private Object handleCacheUpdate(MethodInvocation invocation, CacheUpdate cacheUpdate) throws Throwable {
        // 先执行原方法
        Object value = invocation.proceed();

        String cacheName = cacheUpdate.name();
        if (cacheName.isEmpty()) {
            Method method = invocation.getMethod();
            cacheName = method.getDeclaringClass().getName() + "." + method.getName();
        }

        // 生成缓存key
        String key = keyGenerator.generate(invocation, cacheUpdate.key());

        // 获取缓存
        Cache<String, Object> cache = cacheManager.getCache(cacheName);

        // 更新缓存
        if (value != null) {
            cache.put(key, value);
        }

        return value;
    }

    private Object handleCacheInvalidate(MethodInvocation invocation, CacheInvalidate cacheInvalidate)
            throws Throwable {
        // 先执行原方法
        Object value = invocation.proceed();

        String cacheName = cacheInvalidate.name();
        if (cacheName.isEmpty()) {
            Method method = invocation.getMethod();
            cacheName = method.getDeclaringClass().getName() + "." + method.getName();
        }

        Cache<String, Object> cache = cacheManager.getCache(cacheName);

        if (cacheInvalidate.allEntries()) {
            // 清空所有缓存
            cache.clear();
        } else {
            // 生成缓存key
            String key = keyGenerator.generate(invocation, cacheInvalidate.key());
            // 删除缓存
            cache.remove(key);
        }

        return value;
    }
}