package com.easy.cache.core;

import com.easy.cache.annotation.Cached;
import com.easy.cache.support.DefaultKeyGenerator;
import com.easy.cache.support.KeyGenerator;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * 缓存拦截器，处理缓存注解的核心逻辑
 */
public class CacheInterceptor {

    private final CacheManager cacheManager;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final KeyGenerator keyGenerator;

    /**
     * 构造函数
     */
    public CacheInterceptor(CacheManager cacheManager) {
        this(new DefaultKeyGenerator(), cacheManager);
    }

    public CacheInterceptor(KeyGenerator keyGenerator, CacheManager cacheManager) {
        this.keyGenerator = keyGenerator;
        this.cacheManager = cacheManager;
    }

    /**
     * 处理缓存逻辑
     */
    public Object process(Object target, Method method, Object[] args, Callable<Object> callable) throws Exception {
        Cached cached = method.getAnnotation(Cached.class);
        if (cached == null) {
            return callable.call();
        }

        // 获取缓存键
        String key = parseKey(cached.key(), method, args);

        // 获取缓存
        QuickConfig config = QuickConfig.builder()
                .name(getCacheName(cached, method))
                .expire(cached.expire(), cached.timeUnit())
                .cacheType(convertCacheType(cached.cacheType()))
                .cacheNull(cached.cacheNull())
                .refreshable(cached.refresh())
                .refreshInterval(cached.refreshInterval(), cached.refreshTimeUnit())
                .writeThrough(cached.writeThrough())
                .asyncWrite(cached.asyncWrite())
                .build();

        Cache<String, Object> cache = cacheManager.getOrCreateCache(config);

        // 尝试从缓存获取
        Object value = cache.get(key);
        if (value != null) {
            return value;
        }

        // 调用方法获取结果
        value = callable.call();

        // 缓存结果
        if (value != null || cached.cacheNull()) {
            cache.put(key, value);

            // 如果是可刷新缓存，注册加载器
            if (cached.refresh() && cache instanceof RefreshableCache) {
                ((RefreshableCache<String, Object>) cache).registerLoader(key, k -> {
                    try {
                        return callable.call();
                    } catch (Exception e) {
                        throw new RuntimeException("刷新缓存失败", e);
                    }
                });
            }
        }

        return value;
    }

    /**
     * 解析缓存键
     */
    private String parseKey(String keyExpression, Method method, Object[] args) {
        if (keyExpression == null || keyExpression.isEmpty()) {
            return (String) keyGenerator.generate(null, method, args);
        }

        if (!keyExpression.contains("#")) {
            return keyExpression;
        }

        // 使用Spring的表达式解析器
        org.springframework.context.expression.MethodBasedEvaluationContext context = new org.springframework.context.expression.MethodBasedEvaluationContext(
                null, method, args, parameterNameDiscoverer);

        // 设置参数变量
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
            context.setVariable("arg" + i, args[i]);
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }

    /**
     * 获取缓存名称
     */
    private String getCacheName(Cached cached, Method method) {
        if (cached.name() == null || cached.name().isEmpty()) {
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }
        return cached.name();
    }

    /**
     * 转换缓存类型
     */
    private QuickConfig.CacheType convertCacheType(Cached.CacheType cacheType) {
        switch (cacheType) {
            case REDIS:
                return QuickConfig.CacheType.REMOTE;
            case TWO_LEVEL:
                return QuickConfig.CacheType.BOTH;
            case LOCAL:
            default:
                return QuickConfig.CacheType.LOCAL;
        }
    }
}