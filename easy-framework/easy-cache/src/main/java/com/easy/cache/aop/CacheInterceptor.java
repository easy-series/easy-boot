package com.easy.cache.aop;

import com.easy.cache.annotation.*;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.QuickConfig;
import com.easy.cache.core.RefreshableCache;
import com.easy.cache.support.SpELParser;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 缓存拦截器，用于处理缓存注解
 */
public class CacheInterceptor {

    private final CacheManager cacheManager;
    private final Map<String, Cache<Object, Object>> cacheMap = new ConcurrentHashMap<>();
    private final Map<String, Lock> keyLockMap = new ConcurrentHashMap<>();

    public CacheInterceptor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 处理@Cached注解
     * 
     * @param method   方法
     * @param args     方法参数
     * @param cached   缓存注解
     * @param refresh  刷新注解
     * @param protect  穿透保护注解
     * @param callback 回调函数，用于执行原方法
     * @return 方法返回值
     * @throws Throwable 异常
     */
    @SuppressWarnings("unchecked")
    public Object handleCached(Method method, Object[] args, Cached cached, CacheRefresh refresh,
            CachePenetrationProtect protect, Callback callback) throws Throwable {

        // 获取缓存名称
        String cacheName = getCacheName(method, cached);

        // 获取缓存键
        String cacheKey = getCacheKey(method, args, cached);

        // 获取或创建缓存
        Cache<Object, Object> cache = getOrCreateCache(cacheName, cached, refresh);

        // 尝试从缓存中获取值
        Object value = cache.get(cacheKey);
        if (value != null || (cached.cacheNull() && cache.containsKey(cacheKey))) {
            return value;
        }

        // 如果启用了缓存穿透保护，则使用锁来防止缓存穿透
        if (protect != null) {
            return handleCachePenetrationProtect(method, args, cached, refresh, protect, callback, cacheName, cacheKey,
                    cache);
        }

        // 执行原方法
        value = callback.call();

        // 如果值不为null或者允许缓存null值，则将结果放入缓存
        if (value != null || cached.cacheNull()) {
            cache.put(cacheKey, value, cached.expire(), cached.timeUnit());
        }

        return value;
    }

    /**
     * 处理@CacheUpdate注解
     * 
     * @param method 方法
     * @param args   方法参数
     * @param update 缓存更新注解
     * @param result 方法返回值
     */
    @SuppressWarnings("unchecked")
    public void handleCacheUpdate(Method method, Object[] args, CacheUpdate update, Object result) {
        // 获取缓存名称
        String cacheName = getCacheName(method, update.name());

        // 获取缓存键
        String cacheKey = getExpressionValue(update.key(), method, args, result, String.class);
        if (cacheKey == null) {
            return;
        }

        // 获取缓存值
        Object value = getExpressionValue(update.value(), method, args, result, Object.class);

        // 获取缓存
        Cache<Object, Object> cache = cacheMap.get(cacheName);
        if (cache == null) {
            return;
        }

        // 更新缓存
        cache.put(cacheKey, value);
    }

    /**
     * 处理@CacheInvalidate注解
     * 
     * @param method     方法
     * @param args       方法参数
     * @param invalidate 缓存失效注解
     * @param result     方法返回值
     */
    @SuppressWarnings("unchecked")
    public void handleCacheInvalidate(Method method, Object[] args, CacheInvalidate invalidate, Object result) {
        // 获取缓存名称
        String cacheName = getCacheName(method, invalidate.name());

        // 获取缓存
        Cache<Object, Object> cache = cacheMap.get(cacheName);
        if (cache == null) {
            return;
        }

        // 如果需要清空整个缓存
        if (invalidate.allEntries()) {
            cache.clear();
            return;
        }

        // 获取缓存键
        String cacheKey = getExpressionValue(invalidate.key(), method, args, result, String.class);
        if (cacheKey == null) {
            return;
        }

        // 删除缓存
        cache.remove(cacheKey);
    }

    /**
     * 处理缓存穿透保护
     */
    private Object handleCachePenetrationProtect(Method method, Object[] args, Cached cached, CacheRefresh refresh,
            CachePenetrationProtect protect, Callback callback, String cacheName, String cacheKey,
            Cache<Object, Object> cache) throws Throwable {

        // 获取或创建锁
        Lock lock = keyLockMap.computeIfAbsent(cacheName + ":" + cacheKey, k -> new ReentrantLock());

        // 尝试获取锁
        boolean locked = false;
        try {
            locked = lock.tryLock(protect.timeout(), TimeUnit.MILLISECONDS);
            if (!locked) {
                // 如果获取锁超时，则直接返回null
                return null;
            }

            // 再次尝试从缓存中获取值（可能在等待锁的过程中，其他线程已经将值放入缓存）
            Object value = cache.get(cacheKey);
            if (value != null || (cached.cacheNull() && cache.containsKey(cacheKey))) {
                return value;
            }

            // 执行原方法
            value = callback.call();

            // 如果值不为null或者允许缓存null值，则将结果放入缓存
            if (value != null || cached.cacheNull()) {
                cache.put(cacheKey, value, cached.expire(), cached.timeUnit());
            }

            return value;
        } finally {
            // 释放锁
            if (locked) {
                lock.unlock();
                // 如果不再需要锁，则从map中移除
                keyLockMap.remove(cacheName + ":" + cacheKey);
            }
        }
    }

    /**
     * 获取缓存名称
     */
    private String getCacheName(Method method, Cached cached) {
        String name = cached.name();
        if (name == null || name.isEmpty()) {
            name = method.getDeclaringClass().getName() + "." + method.getName();
        }
        return name;
    }

    /**
     * 获取缓存名称
     */
    private String getCacheName(Method method, String name) {
        if (name == null || name.isEmpty()) {
            name = method.getDeclaringClass().getName() + "." + method.getName();
        }
        return name;
    }

    /**
     * 获取缓存键
     */
    private String getCacheKey(Method method, Object[] args, Cached cached) {
        String key = cached.key();
        if (key == null || key.isEmpty()) {
            // 如果没有指定key，则使用默认的key生成策略
            return SpELParser.generateDefaultKey(method, args);
        }

        // 解析SpEL表达式
        String cacheKey = getExpressionValue(key, method, args, null, String.class);
        if (cacheKey == null) {
            // 如果解析失败，则使用默认的key生成策略
            return SpELParser.generateDefaultKey(method, args);
        }

        return cacheKey;
    }

    /**
     * 获取表达式值
     */
    private <T> T getExpressionValue(String expression, Method method, Object[] args, Object result,
            Class<T> resultType) {
        return SpELParser.parse(expression, method, args, result, resultType);
    }

    /**
     * 获取或创建缓存
     */
    @SuppressWarnings("unchecked")
    private Cache<Object, Object> getOrCreateCache(String cacheName, Cached cached, CacheRefresh refresh) {
        return cacheMap.computeIfAbsent(cacheName, name -> {
            // 创建缓存配置
            QuickConfig config = createCacheConfig(name, cached);

            // 创建缓存
            Cache<Object, Object> cache = (Cache<Object, Object>) cacheManager.getOrCreateCache(config);

            // 如果需要自动刷新，则创建可刷新缓存
            if (refresh != null) {
                RefreshableCache<Object, Object> refreshableCache = new RefreshableCache<>(cache,
                        refresh.refresh(), refresh.timeUnit(), 2);
                refreshableCache.setStopRefreshAfterLastAccess(refresh.stopRefreshAfterLastAccess());
                return refreshableCache;
            }

            return cache;
        });
    }

    /**
     * 创建缓存配置
     */
    private QuickConfig createCacheConfig(String name, Cached cached) {
        return QuickConfig.builder()
                .name(name)
                .cacheType(cached.cacheType())
                .expire(cached.expire(), cached.timeUnit())
                .cacheNull(cached.cacheNull())
                .writeThrough(cached.writeThrough())
                .asyncWrite(cached.asyncWrite())
                .syncLocal(cached.syncLocal())
                .localLimit(cached.localLimit())
                .build();
    }

    /**
     * 回调接口，用于执行原方法
     */
    public interface Callback {
        Object call() throws Throwable;
    }
}