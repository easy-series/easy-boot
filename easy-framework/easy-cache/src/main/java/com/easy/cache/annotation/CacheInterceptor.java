package com.easy.cache.annotation;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheBuilder;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.RefreshableCache;
import com.easy.cache.support.DefaultKeyGenerator;
import com.easy.cache.support.KeyGenerator;
import com.easy.cache.support.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * 缓存注解处理器
 */
public class CacheInterceptor {

    private final KeyGenerator keyGenerator;
    private final CacheManager cacheManager;

    public CacheInterceptor() {
        this(new DefaultKeyGenerator(), CacheManager.getInstance());
    }

    public CacheInterceptor(KeyGenerator keyGenerator, CacheManager cacheManager) {
        this.keyGenerator = keyGenerator;
        this.cacheManager = cacheManager;
    }

    /**
     * 处理缓存注解
     * 
     * @param target  目标对象
     * @param method  方法
     * @param args    方法参数
     * @param invoker 方法调用器
     * @return 方法返回值
     * @throws Exception 异常
     */
    public Object process(Object target, Method method, Object[] args, MethodInvoker invoker) throws Exception {
        Cached cached = method.getAnnotation(Cached.class);
        if (cached == null) {
            return invoker.invoke();
        }

        String cacheName = getCacheName(cached, method);
        Object key = getKey(cached, target, method, args);

        // 创建或获取缓存
        Cache<Object, Object> cache = getCache(cached, cacheName);

        // 获取缓存值
        Object value = cache.get(key);

        if (value != null) {
            return value;
        }

        // 调用方法获取结果
        value = invoker.invoke();

        if (value != null || cached.cacheNull()) {
            // 如果需要自动刷新，注册加载器
            if (cached.refresh() && cache instanceof RefreshableCache) {
                final Object finalValue = value;
                final MethodInvoker finalInvoker = invoker;

                // 创建加载器
                Function<Object, Object> loader = k -> {
                    try {
                        return finalInvoker.invoke();
                    } catch (Exception e) {
                        // 刷新失败，返回旧值
                        return finalValue;
                    }
                };

                // 注册加载器
                ((RefreshableCache<Object, Object>) cache).registerLoader(key, loader);
            }

            // 放入缓存
            cache.put(key, value, cached.expire(), cached.timeUnit());
        }

        return value;
    }

    /**
     * 获取缓存名称
     */
    private String getCacheName(Cached cached, Method method) {
        String name = cached.name();
        if (name.isEmpty()) {
            name = method.getDeclaringClass().getName() + "." + method.getName();
        }
        return name;
    }

    /**
     * 获取缓存键
     */
    private Object getKey(Cached cached, Object target, Method method, Object[] args) {
        String key = cached.key();
        if (!key.isEmpty()) {
            // 解析SpEL表达式
            return SpelExpressionParser.parseExpression(key, method, args, target);
        }
        return keyGenerator.generate(target, method, args);
    }

    /**
     * 获取缓存实例
     */
    @SuppressWarnings("unchecked")
    private Cache<Object, Object> getCache(Cached cached, String cacheName) {
        // 创建缓存构建器
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
                .name(cacheName)
                .expireAfterWrite(cached.expire(), cached.timeUnit())
                .allowNullValues(cached.cacheNull());

        // 设置缓存类型
        switch (cached.cacheType()) {
            case REDIS:
                builder.useRedis();
                break;
            case TWO_LEVEL:
                builder.useTwoLevel();
                break;
            case LOCAL:
            default:
                // 默认使用本地缓存
                break;
        }

        // 设置自动刷新
        if (cached.refresh()) {
            builder.refreshable()
                    .refreshInterval(cached.refreshInterval(), cached.refreshTimeUnit());
        }

        // 构建缓存
        return builder.build();
    }

    /**
     * 方法调用器接口
     */
    public interface MethodInvoker {
        Object invoke() throws Exception;
    }
}