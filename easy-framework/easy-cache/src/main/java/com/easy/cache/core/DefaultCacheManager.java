package com.easy.cache.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.RedisTemplate;

import com.easy.cache.api.Cache;
import com.easy.cache.api.CacheManager;
import com.easy.cache.config.CacheConfig;
import com.easy.cache.config.CacheType;
import com.easy.cache.config.QuickConfig;
import com.easy.cache.core.embedded.CaffeineCache;
import com.easy.cache.core.embedded.LocalCache;
import com.easy.cache.core.multi.MultiLevelCache;
import com.easy.cache.core.redis.RedisCache;
import com.easy.cache.support.convertor.KeyConvertor;
import com.easy.cache.support.serializer.Serializer;
import com.easy.cache.support.sync.CacheEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认缓存管理器实现
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCacheManager implements CacheManager {

    /**
     * 缓存注册表
     */
    private final Map<String, Cache<?, ?>> cacheRegistry = new ConcurrentHashMap<>();

    /**
     * 键转换器
     */
    private final KeyConvertor keyConvertor;

    /**
     * 值序列化器
     */
    private final Serializer serializer;

    /**
     * Redis模板
     */
    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * 缓存事件发布器
     */
    private final CacheEventPublisher eventPublisher;

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) cacheRegistry.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(CacheConfig<K, V> config) {
        String name = config.getName();
        Cache<?, ?> cache = cacheRegistry.get(name);
        if (cache != null) {
            return (Cache<K, V>) cache;
        }

        // 创建新缓存
        cache = createCache(config);
        cacheRegistry.put(name, cache);
        return (Cache<K, V>) cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(QuickConfig quickConfig) {
        String name = quickConfig.getName();
        Cache<?, ?> cache = cacheRegistry.get(name);
        if (cache != null) {
            return (Cache<K, V>) cache;
        }

        // 将QuickConfig转换为CacheConfig
        CacheConfig<K, V> config = CacheConfig.<K, V>builder()
                .name(quickConfig.getName())
                .cacheType(quickConfig.getCacheType())
                .keyConvertor(keyConvertor)
                .valueSerializer(serializer)
                .expireAfterWrite(quickConfig.getExpire())
                .localLimit(quickConfig.getLocalLimit())
                .syncLocal(quickConfig.isSyncLocal())
                .syncStrategy(quickConfig.getSyncStrategy())
                .cacheNullValues(quickConfig.isCacheNullValues())
                .penetrationProtect(quickConfig.isPenetrationProtect())
                .writeThrough(quickConfig.isWriteThrough())
                .asyncWrite(quickConfig.isAsyncWrite())
                .build();

        // 创建新缓存
        cache = createCache(config);
        cacheRegistry.put(name, cache);
        return (Cache<K, V>) cache;
    }

    @Override
    public void removeCache(String name) {
        Cache<?, ?> cache = cacheRegistry.remove(name);
        if (cache != null) {
            log.debug("移除缓存: name={}", name);
        }
    }

    @Override
    public void clear() {
        cacheRegistry.clear();
        log.debug("清空所有缓存");
    }

    /**
     * 创建缓存实例
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 缓存实例
     */
    private <K, V> Cache<K, V> createCache(CacheConfig<K, V> config) {
        CacheType cacheType = config.getCacheType();

        switch (cacheType) {
            case LOCAL:
                return createLocalCache(config);
            case REMOTE:
                return createRemoteCache(config);
            case BOTH:
                return createMultiLevelCache(config);
            default:
                throw new IllegalArgumentException("不支持的缓存类型: " + cacheType);
        }
    }

    /**
     * 创建本地缓存
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 本地缓存实例
     */
    private <K, V> LocalCache<K, V> createLocalCache(CacheConfig<K, V> config) {
        return new CaffeineCache<>(
                config.getName(),
                config.getKeyConvertor() != null ? config.getKeyConvertor() : keyConvertor,
                config.getExpireAfterWrite(),
                config.getExpireAfterAccess(),
                config.getRefreshAfterWrite(),
                config.getInitialCapacity(),
                config.getLocalLimit(),
                config.isRecordStats(),
                config.getLoader(),
                config.isCacheNullValues(),
                config.isPenetrationProtect());
    }

    /**
     * 创建远程缓存
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 远程缓存实例
     */
    private <K, V> RedisCache<K, V> createRemoteCache(CacheConfig<K, V> config) {
        return new RedisCache<>(
                config.getName(),
                config.getKeyConvertor() != null ? config.getKeyConvertor() : keyConvertor,
                redisTemplate,
                config.getValueSerializer() != null ? config.getValueSerializer() : serializer,
                config.getExpireAfterWrite(),
                config.getLoader(),
                config.isCacheNullValues(),
                config.isPenetrationProtect(),
                eventPublisher,
                config.isSyncLocal());
    }

    /**
     * 创建多级缓存
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 多级缓存实例
     */
    private <K, V> MultiLevelCache<K, V> createMultiLevelCache(CacheConfig<K, V> config) {
        // 创建本地缓存和远程缓存
        LocalCache<K, V> localCache = createLocalCache(config);
        RedisCache<K, V> remoteCache = createRemoteCache(config);

        return new MultiLevelCache<>(
                config.getName(),
                config.getKeyConvertor() != null ? config.getKeyConvertor() : keyConvertor,
                localCache,
                remoteCache,
                config.getExpireAfterWrite(),
                config.getLoader(),
                config.isCacheNullValues(),
                config.isPenetrationProtect(),
                config.isWriteThrough(),
                config.isAsyncWrite());
    }
}