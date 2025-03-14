package com.easy.cache.aop;

import com.easy.cache.annotation.CacheEvict;
import com.easy.cache.annotation.CachePut;
import com.easy.cache.annotation.Cacheable;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 缓存切面，负责拦截缓存注解并进行相应处理
 */
@Aspect
@Component
public class CacheAspect {

    @Autowired
    private ApplicationContext applicationContext;

    private CacheKeyGenerator keyGenerator = new DefaultCacheKeyGenerator();

    /**
     * 处理 @Cacheable 注解
     */
    @Around("@annotation(com.easy.cache.annotation.Cacheable)")
    public Object cacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法信息
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();

        // 获取注解
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        if (cacheable == null) {
            return joinPoint.proceed();
        }

        // 检查条件表达式
        if (!checkCondition(cacheable.condition(), method, args, target, null)) {
            return joinPoint.proceed();
        }

        // 获取缓存名称
        String cacheName = cacheable.cacheName();
        if (cacheable.useMultiLevel()) {
            // 使用多级缓存
            cacheName = CacheManager.getInstance().getOrCreateMultiLevelCache(cacheName,
                    cacheName + ":local", cacheName + ":redis").getName();
        }

        // 获取缓存实例
        Cache<Object, Object> cache = getCache(cacheName);
        if (cache == null) {
            return joinPoint.proceed();
        }

        // 生成缓存键
        Object cacheKey = generateKey(cacheable.key(), cacheable.keyGenerator(), method, args, target, null);

        // 从缓存中获取数据
        Object cacheValue = cache.get(cacheKey);
        if (cacheValue != null) {
            // 缓存命中
            System.out.println("缓存命中: " + cacheName + ", key=" + cacheKey);
            return cacheValue;
        }

        // 缓存未命中，执行方法并缓存结果
        Object result = joinPoint.proceed();
        if (result != null) {
            // 设置缓存
            if (cacheable.expireTime() > 0) {
                cache.put(cacheKey, result, cacheable.expireTime());
            } else {
                cache.put(cacheKey, result);
            }
            System.out.println("缓存方法结果: " + cacheName + ", key=" + cacheKey);
        }

        return result;
    }

    /**
     * 处理 @CachePut 注解
     */
    @Around("@annotation(com.easy.cache.annotation.CachePut)")
    public Object cachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法信息
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();

        // 执行方法
        Object result = joinPoint.proceed();

        // 获取注解
        CachePut cachePut = method.getAnnotation(CachePut.class);
        if (cachePut == null) {
            return result;
        }

        // 检查条件表达式
        if (!checkCondition(cachePut.condition(), method, args, target, result)) {
            return result;
        }

        // 获取缓存名称
        String cacheName = cachePut.cacheName();
        if (cachePut.useMultiLevel()) {
            // 使用多级缓存
            cacheName = CacheManager.getInstance().getOrCreateMultiLevelCache(cacheName,
                    cacheName + ":local", cacheName + ":redis").getName();
        }

        // 获取缓存实例
        Cache<Object, Object> cache = getCache(cacheName);
        if (cache == null) {
            return result;
        }

        // 生成缓存键
        Object cacheKey = generateKey(cachePut.key(), cachePut.keyGenerator(), method, args, target, result);

        // 更新缓存
        if (result != null) {
            if (cachePut.expireTime() > 0) {
                cache.put(cacheKey, result, cachePut.expireTime());
            } else {
                cache.put(cacheKey, result);
            }
            System.out.println("更新缓存: " + cacheName + ", key=" + cacheKey);
        }

        return result;
    }

    /**
     * 处理 @CacheEvict 注解
     */
    @Around("@annotation(com.easy.cache.annotation.CacheEvict)")
    public Object cacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法信息
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();

        // 获取注解
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
        if (cacheEvict == null) {
            return joinPoint.proceed();
        }

        // 如果在方法执行前清除缓存
        if (cacheEvict.beforeInvocation()) {
            evictCache(cacheEvict, method, args, target, null);
            return joinPoint.proceed();
        }

        // 执行方法
        Object result = joinPoint.proceed();

        // 在方法执行后清除缓存
        evictCache(cacheEvict, method, args, target, result);

        return result;
    }

    /**
     * 清除缓存
     */
    private void evictCache(CacheEvict cacheEvict, Method method, Object[] args, Object target, Object result) {
        // 检查条件表达式
        if (!checkCondition(cacheEvict.condition(), method, args, target, result)) {
            return;
        }

        // 获取缓存名称
        String cacheName = cacheEvict.cacheName();

        // 获取缓存实例
        Cache<Object, Object> cache = getCache(cacheName);
        if (cache == null) {
            return;
        }

        if (cacheEvict.allEntries()) {
            // 清除所有缓存
            cache.clear();
            System.out.println("清除所有缓存: " + cacheName);
        } else {
            // 生成缓存键
            Object cacheKey = generateKey(cacheEvict.key(), cacheEvict.keyGenerator(), method, args, target, result);

            // 清除指定键的缓存
            cache.remove(cacheKey);
            System.out.println("清除缓存: " + cacheName + ", key=" + cacheKey);
        }
    }

    /**
     * 获取缓存实例
     */
    @SuppressWarnings("unchecked")
    private Cache<Object, Object> getCache(String cacheName) {
        try {
            return (Cache<Object, Object>) CacheManager.getInstance().getCache(cacheName);
        } catch (Exception e) {
            System.err.println("获取缓存失败: " + cacheName + ", " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成缓存键
     */
    private Object generateKey(String key, String keyGeneratorName, Method method, Object[] args, Object target,
            Object result) {
        // 如果指定了键生成器
        if (keyGeneratorName != null && !keyGeneratorName.isEmpty()) {
            try {
                CacheKeyGenerator customKeyGenerator = applicationContext.getBean(keyGeneratorName,
                        CacheKeyGenerator.class);
                return customKeyGenerator.generate(target, method, args);
            } catch (Exception e) {
                System.err.println("获取键生成器失败: " + keyGeneratorName + ", " + e.getMessage());
            }
        }

        // 如果指定了键表达式
        if (key != null && !key.isEmpty()) {
            // 创建表达式上下文
            EvaluationContext context = DefaultCacheKeyGenerator.createEvaluationContext(method, args, target, result);
            // 解析表达式
            Object keyValue = keyGenerator.parseExpression(key, context);
            if (keyValue != null) {
                return keyValue;
            }
        }

        // 使用默认键生成器
        return keyGenerator.generate(target, method, args);
    }

    /**
     * 检查条件表达式
     */
    private boolean checkCondition(String condition, Method method, Object[] args, Object target, Object result) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        try {
            // 创建表达式上下文
            EvaluationContext context = DefaultCacheKeyGenerator.createEvaluationContext(method, args, target, result);
            // 解析条件表达式
            Object value = keyGenerator.parseExpression(condition, context);
            return value instanceof Boolean ? (Boolean) value : true;
        } catch (Exception e) {
            System.err.println("解析条件表达式失败: " + condition + ", " + e.getMessage());
            return false;
        }
    }
}