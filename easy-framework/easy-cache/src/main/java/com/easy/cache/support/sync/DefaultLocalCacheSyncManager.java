package com.easy.cache.support.sync;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.easy.cache.core.embedded.LocalCache;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认的本地缓存同步管理器
 */
@Slf4j
public class DefaultLocalCacheSyncManager implements LocalCacheSyncManager {

    /**
     * 缓存事件订阅者注册表
     */
    private final Map<String, LocalCache<?, ?>> localCacheRegistry = new ConcurrentHashMap<>();

    @Override
    public void registerLocalCache(String cacheName, LocalCache<?, ?> cache) {
        localCacheRegistry.put(cacheName, cache);
        log.debug("注册本地缓存同步: {}", cacheName);
    }

    @Override
    public void unregisterLocalCache(String cacheName) {
        localCacheRegistry.remove(cacheName);
        log.debug("取消注册本地缓存同步: {}", cacheName);
    }

    @Override
    public void handleCacheEvent(CacheEvent event) {
        String cacheName = event.getCacheName();
        LocalCache<?, ?> cache = localCacheRegistry.get(cacheName);

        if (cache == null) {
            return;
        }

        switch (event.getEventType()) {
            case PUT:
            case UPDATE:
                String key = event.getCacheKey();
                if (key != null) {
                    cache.invalidate(key);
                    log.debug("同步更新本地缓存: name={}, key={}", cacheName, key);
                }
                break;
            case REMOVE:
                key = event.getCacheKey();
                if (key != null) {
                    cache.invalidate(key);
                    log.debug("同步删除本地缓存: name={}, key={}", cacheName, key);
                }
                break;
            case CLEAR:
                cache.clear();
                log.debug("同步清空本地缓存: name={}", cacheName);
                break;
            default:
                log.warn("未知缓存事件类型: {}", event.getEventType());
        }
    }
}