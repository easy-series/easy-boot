package com.easy.cache.aop;

import com.easy.cache.annotation.*;
import com.easy.cache.core.*;
import com.easy.cache.key.KeyGenerator;
import com.easy.cache.key.SpELParser;
import com.easy.cache.key.SpelKeyGenerator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

/**
 * 缓存切面，用于拦截缓存注解
 */
@Aspect
public class CacheAspect implements Ordered {

    /**
     * 缓存管理器
     */
    private CacheManager cacheManager;

    /**
     * Spring应用上下文
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 默认键生成器
     */
    private KeyGenerator keyGenerator;

    /**
     * SpEL表达式解析器
     */
    private SpELParser spELParser;

    /**
     * 设置缓存管理器
     *
     * @param cacheManager 缓存管理器
     */
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 设置键生成器
     *
     * @param keyGenerator 键生成器
     */
    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    /**
     * 设置SpEL表达式解析器
     *
     * @param spELParser SpEL表达式解析器
     */
    public void setSpELParser(SpELParser spELParser) {
        this.spELParser = spELParser;
    }

    /**
     * 拦截@Cached注解
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(com.easy.cache.annotation.Cached)")
    public Object cached(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取缓存注解
        Cached cachedAnnotation = method.getAnnotation(Cached.class);
        
        // 获取热点缓存注解
        HotKeyProtect hotKeyProtect = method.getAnnotation(HotKeyProtect.class);
        
        // 获取缓存穿透保护注解
        CachePenetrationProtect penetrationProtect = method.getAnnotation(CachePenetrationProtect.class);
        
        // 获取缓存刷新注解
        CacheRefresh cacheRefresh = method.getAnnotation(CacheRefresh.class);
        
        // 获取缓存名称和类型
        String cacheName = cachedAnnotation.name();
        CacheType cacheType = cachedAnnotation.cacheType();
        
        // 创建缓存键
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        String key = generateKey(cachedAnnotation.key(), cachedAnnotation.keyGenerator(), target, method, args);
        
        // 获取基础缓存
        Cache<String, Object> cache = cacheManager.getOrCreateCache(cacheName, cacheType);
        
        // 应用热点保护
        if (hotKeyProtect != null) {
            cache = new HotKeyCache<>(
                    cache,
                    hotKeyProtect.threshold(),
                    hotKeyProtect.windowInMillis(),
                    hotKeyProtect.localCacheSeconds()
            );
        }
        
        // 应用缓存穿透保护
        if (penetrationProtect != null) {
            cache = new BloomFilterCache<>(
                    cache,
                    penetrationProtect.expectedSize(),
                    penetrationProtect.fpp(),
                    null
            );
        }
        
        // 从缓存获取值
        Object value = cache.get(key);
        
        // 如果缓存命中，则直接返回
        if (value != null) {
            return value;
        }
        
        // 缓存未命中，执行方法获取结果
        Object result = joinPoint.proceed();
        
        // 如果结果为null且不缓存null值，则直接返回
        if (result == null && !cachedAnnotation.cacheNull()) {
            return null;
        }
        
        // 缓存方法结果
        if (cacheRefresh != null) {
            // 如果配置了缓存刷新，则创建可刷新缓存
            Function<String, Object> valueLoader = k -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to refresh cache", e);
                }
            };
            
            RefreshableCache<String, Object> refreshableCache = new RefreshableCache<>(
                    cache,
                    valueLoader,
                    cacheRefresh.randomDelay(),
                    cacheRefresh.maxRandomDelay()
            );
            
            refreshableCache.putWithRefresh(
                    key,
                    result,
                    cachedAnnotation.expire(),
                    cachedAnnotation.timeUnit(),
                    cacheRefresh.refresh(),
                    cacheRefresh.timeUnit()
            );
        } else {
            // 简单缓存
            cache.put(key, result, cachedAnnotation.expire(), cachedAnnotation.timeUnit());
        }
        
        return result;
    }

    /**
     * 拦截@CacheUpdate注解
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(com.easy.cache.annotation.CacheUpdate)")
    public Object cacheUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        // 先执行方法
        Object result = joinPoint.proceed();
        
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取缓存更新注解
        CacheUpdate cacheUpdate = method.getAnnotation(CacheUpdate.class);
        
        // 获取缓存名称
        String cacheName = cacheUpdate.name();
        
        // 从缓存管理器获取缓存
        Cache<String, Object> cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return result;
        }
        
        // 创建缓存键
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        String key = generateKeyWithResult(cacheUpdate.key(), target, method, args, result);
        
        // 创建缓存值
        Object value = result;
        if (!cacheUpdate.value().equals("#result")) {
            value = spELParser.parse(cacheUpdate.value(), method, args, target, result);
        }
        
        // 更新缓存
        cache.put(key, value);
        
        return result;
    }

    /**
     * 拦截@CacheInvalidate注解
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(com.easy.cache.annotation.CacheInvalidate)")
    public Object cacheInvalidate(ProceedingJoinPoint joinPoint) throws Throwable {
        // 先执行方法
        Object result = joinPoint.proceed();
        
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取缓存失效注解
        CacheInvalidate cacheInvalidate = method.getAnnotation(CacheInvalidate.class);
        
        // 获取缓存名称
        String cacheName = cacheInvalidate.name();
        
        // 从缓存管理器获取缓存
        Cache<String, Object> cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return result;
        }
        
        // 如果配置了清除所有缓存
        if (cacheInvalidate.clearAll()) {
            cache.clear();
            return result;
        }
        
        // 创建缓存键
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        String key = generateKeyWithResult(cacheInvalidate.key(), target, method, args, result);
        
        // 删除缓存
        cache.remove(key);
        
        return result;
    }

    /**
     * 生成缓存键
     *
     * @param keyExpression 键表达式
     * @param keyGeneratorName 键生成器名称
     * @param target 目标对象
     * @param method 方法
     * @param args 方法参数
     * @return 缓存键
     */
    private String generateKey(String keyExpression, String keyGeneratorName, Object target, Method method, Object[] args) {
        // 如果指定了自定义键生成器
        if (StringUtils.hasText(keyGeneratorName)) {
            KeyGenerator customKeyGenerator = applicationContext.getBean(keyGeneratorName, KeyGenerator.class);
            return customKeyGenerator.generate(target, method, args);
        }
        
        // 如果指定了SpEL表达式
        if (StringUtils.hasText(keyExpression)) {
            SpelKeyGenerator spelKeyGenerator = new SpelKeyGenerator(keyExpression);
            return spelKeyGenerator.generate(target, method, args);
        }
        
        // 使用默认键生成器
        return keyGenerator.generate(target, method, args);
    }
    
    /**
     * 生成包含方法执行结果的缓存键
     *
     * @param keyExpression 键表达式
     * @param target 目标对象
     * @param method 方法
     * @param args 方法参数
     * @param result 方法执行结果
     * @return 缓存键
     */
    private String generateKeyWithResult(String keyExpression, Object target, Method method, Object[] args, Object result) {
        // 使用SpEL表达式解析器解析表达式
        Object key = spELParser.parse(keyExpression, method, args, target, result);
        return Objects.toString(key, "null");
    }
    
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 1;
    }
} 