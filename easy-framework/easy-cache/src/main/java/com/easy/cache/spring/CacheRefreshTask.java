package com.easy.cache.spring;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.easy.cache.annotation.CacheRefresh;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;

/**
 * 缓存刷新任务
 */
@Component
public class CacheRefreshTask {

    @Autowired
    private CacheManager cacheManager;

    private final Map<String, Method> refreshMethods = new ConcurrentHashMap<>();

    public void registerRefreshMethod(String cacheName, Method method) {
        refreshMethods.put(cacheName, method);
    }

    @Scheduled(fixedRate = 1000)
    public void refreshCache() {
        for (Map.Entry<String, Method> entry : refreshMethods.entrySet()) {
            String cacheName = entry.getKey();
            Method method = entry.getValue();

            CacheRefresh refresh = method.getAnnotation(CacheRefresh.class);
            if (refresh == null) {
                continue;
            }

            // 检查是否需要刷新
            if (!shouldRefresh(refresh)) {
                continue;
            }

            // 执行刷新方法
            try {
                Object result = method.invoke(null);
                if (result != null) {
                    Cache<Object, Object> cache = cacheManager.getCache(cacheName);
                    cache.put(refresh.key(), result);
                }
            } catch (Exception e) {
                // 记录错误日志
                e.printStackTrace();
            }
        }
    }

    private boolean shouldRefresh(CacheRefresh refresh) {
        // TODO: 实现刷新时间检查逻辑
        return true;
    }
}