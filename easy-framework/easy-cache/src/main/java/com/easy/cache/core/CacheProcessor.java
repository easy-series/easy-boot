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
 * 缓存处理器，处理缓存注解的核心逻辑
 */
public class CacheProcessor {

    private final CacheManager cacheManager;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final KeyGenerator keyGenerator;

    /**
     * 构造函数
     */
    public CacheProcessor(CacheManager cacheManager) {
        this(new DefaultKeyGenerator(), cacheManager);
    }

    public CacheProcessor(KeyGenerator keyGenerator, CacheManager cacheManager) {
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
                .cacheType(cached.cacheType())
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

    // 其他方法保持不变...
} 