package com.easy.cache.annotation;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.CacheType;
import com.easy.cache.core.SpelKeyGenerator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * 缓存切面，用于处理缓存相关注解
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(CacheAspect.class);

    /**
     * 缓存管理器
     */
    private final CacheManager cacheManager;

    /**
     * 基于SpEL的键生成器
     */
    private final SpelKeyGenerator spelKeyGenerator;

    /**
     * 构造函数
     *
     * @param cacheManager 缓存管理器
     * @param spelKeyGenerator 基于SpEL的键生成器
     */
    public CacheAspect(CacheManager cacheManager, SpelKeyGenerator spelKeyGenerator) {
        this.cacheManager = cacheManager;
        this.spelKeyGenerator = spelKeyGenerator;
    }

    /**
     * 处理@Cacheable注解
     */
    @Around("@annotation(com.easy.cache.annotation.Cacheable)")
    public Object cacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        String cacheName = cacheable.cacheName();
        String key = generateKey(cacheable.key(), joinPoint, method);
        CacheType cacheType = cacheable.cacheType();

        // 获取缓存
        Cache<String, Object> cache = cacheManager.getCache(cacheName, cacheType);

        // 尝试从缓存获取值
        Object value = cache.get(key);
        if (value != null) {
            logger.debug("Cache hit: cacheName={}, key={}", cacheName, key);
            return value;
        }

        // 缓存未命中，执行方法
        logger.debug("Cache miss: cacheName={}, key={}", cacheName, key);
        value = joinPoint.proceed();

        // 结果不为null时才缓存
        if (value != null) {
            cache.put(key, value);
            logger.debug("Cache put: cacheName={}, key={}", cacheName, key);
        }

        return value;
    }

    /**
     * 处理@CachePut注解
     */
    @Around("@annotation(com.easy.cache.annotation.CachePut)")
    public Object cachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CachePut cachePut = method.getAnnotation(CachePut.class);

        String cacheName = cachePut.cacheName();
        String key = generateKey(cachePut.key(), joinPoint, method);
        CacheType cacheType = cachePut.cacheType();

        // 获取缓存
        Cache<String, Object> cache = cacheManager.getCache(cacheName, cacheType);

        // 执行方法
        Object value = joinPoint.proceed();

        // 结果不为null时才缓存
        if (value != null) {
            cache.put(key, value);
            logger.debug("Cache put: cacheName={}, key={}", cacheName, key);
        }

        return value;
    }

    /**
     * 处理@CacheEvict注解
     */
    @Around("@annotation(com.easy.cache.annotation.CacheEvict)")
    public Object cacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);

        String cacheName = cacheEvict.cacheName();
        CacheType cacheType = cacheEvict.cacheType();
        boolean allEntries = cacheEvict.allEntries();
        boolean beforeInvocation = cacheEvict.beforeInvocation();

        // 获取缓存
        Cache<String, Object> cache = cacheManager.getCache(cacheName, cacheType);

        // 在方法执行前清除缓存
        if (beforeInvocation) {
            evictCache(cache, cacheEvict, joinPoint, method, allEntries);
        }

        // 执行方法
        Object result = joinPoint.proceed();

        // 在方法执行后清除缓存
        if (!beforeInvocation) {
            evictCache(cache, cacheEvict, joinPoint, method, allEntries);
        }

        return result;
    }

    /**
     * 处理@Caching注解
     */
    @Around("@annotation(com.easy.cache.annotation.Caching)")
    public Object caching(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Caching caching = method.getAnnotation(Caching.class);

        // 执行方法
        Object result = joinPoint.proceed();

        // 处理所有的@CachePut注解
        for (CachePut cachePut : caching.put()) {
            String cacheName = cachePut.cacheName();
            String key = generateKey(cachePut.key(), joinPoint, method);
            CacheType cacheType = cachePut.cacheType();

            Cache<String, Object> cache = cacheManager.getCache(cacheName, cacheType);
            if (result != null) {
                cache.put(key, result);
                logger.debug("Cache put (Caching): cacheName={}, key={}", cacheName, key);
            }
        }

        // 处理所有的@CacheEvict注解
        for (CacheEvict cacheEvict : caching.evict()) {
            String cacheName = cacheEvict.cacheName();
            CacheType cacheType = cacheEvict.cacheType();
            boolean allEntries = cacheEvict.allEntries();

            Cache<String, Object> cache = cacheManager.getCache(cacheName, cacheType);
            evictCache(cache, cacheEvict, joinPoint, method, allEntries);
        }

        return result;
    }

    /**
     * 处理@CacheRefresh注解
     */
    @Around("@annotation(com.easy.cache.annotation.CacheRefresh)")
    public Object cacheRefresh(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheRefresh cacheRefresh = method.getAnnotation(CacheRefresh.class);

        String cacheName = cacheRefresh.cacheName();
        String key = generateKey(cacheRefresh.key(), joinPoint, method);
        CacheType cacheType = cacheRefresh.cacheType();
        boolean async = cacheRefresh.async();

        // 获取缓存
        Cache<String, Object> cache = cacheManager.getCache(cacheName, cacheType);

        if (async) {
            // 异步执行方法并刷新缓存
            CompletableFuture.runAsync(() -> {
                try {
                    Object value = joinPoint.proceed();
                    if (value != null) {
                        cache.put(key, value);
                        logger.debug("Cache refreshed asynchronously: cacheName={}, key={}", cacheName, key);
                    }
                } catch (Throwable e) {
                    logger.error("Error refreshing cache asynchronously: " + e.getMessage(), e);
                }
            });
            return cache.get(key);
        } else {
            // 同步执行方法并刷新缓存
            Object value = joinPoint.proceed();
            if (value != null) {
                cache.put(key, value);
                logger.debug("Cache refreshed: cacheName={}, key={}", cacheName, key);
            }
            return value;
        }
    }

    /**
     * 生成缓存键
     *
     * @param keyExpression 键表达式
     * @param joinPoint 连接点
     * @param method 方法
     * @return 缓存键
     */
    private String generateKey(String keyExpression, ProceedingJoinPoint joinPoint, Method method) {
        if (StringUtils.hasText(keyExpression)) {
            return spelKeyGenerator.generate(keyExpression, joinPoint, method);
        } else {
            // 默认使用方法名+参数作为键
            StringBuilder sb = new StringBuilder();
            sb.append(method.getDeclaringClass().getName()).append(".").append(method.getName());
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg != null) {
                    sb.append(":").append(arg.toString());
                }
            }
            return sb.toString();
        }
    }

    /**
     * 清除缓存
     *
     * @param cache 缓存
     * @param cacheEvict 缓存清除注解
     * @param joinPoint 连接点
     * @param method 方法
     * @param allEntries 是否清除所有条目
     */
    private void evictCache(Cache<String, Object> cache, CacheEvict cacheEvict, ProceedingJoinPoint joinPoint, Method method, boolean allEntries) {
        if (allEntries) {
            cache.clear();
            logger.debug("Cache cleared: cacheName={}", cacheEvict.cacheName());
        } else {
            String key = generateKey(cacheEvict.key(), joinPoint, method);
            cache.remove(key);
            logger.debug("Cache evicted: cacheName={}, key={}", cacheEvict.cacheName(), key);
        }
    }
} 