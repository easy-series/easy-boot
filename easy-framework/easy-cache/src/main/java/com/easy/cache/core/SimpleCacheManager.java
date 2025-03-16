package com.easy.cache.core;

import com.easy.cache.config.CacheConfig;
import com.easy.cache.config.QuickConfig;
import com.easy.cache.exception.CacheException;
import com.easy.cache.local.caffeine.CaffeineCache;
import com.easy.cache.local.linkedhashmap.LinkedHashMapCache;
import com.easy.cache.serialization.KeyConvertor;
import com.easy.cache.serialization.ValueDecoder;
import com.easy.cache.serialization.ValueEncoder;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单缓存管理器实现
 */
public class SimpleCacheManager implements CacheManager {
    
    /**
     * 管理器名称
     */
    private static final String MANAGER_NAME = "SimpleCacheManager";
    
    /**
     * 缓存实例Map
     */
    private final Map<String, Cache<?, ?>> cacheMap = new ConcurrentHashMap<>();
    
    /**
     * 键转换器
     */
    @Autowired(required = false)
    private KeyConvertor keyConvertor;
    
    /**
     * 值编码器
     */
    @Autowired(required = false)
    private ValueEncoder valueEncoder;
    
    /**
     * 值解码器
     */
    @Autowired(required = false)
    private ValueDecoder<?> valueDecoder;
    
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) cacheMap.get(name);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(CacheConfig<K, V> config) {
        String name = config.getName();
        return (Cache<K, V>) cacheMap.computeIfAbsent(name, k -> createCache(name, config));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(QuickConfig config) {
        String name = config.getName();
        return (Cache<K, V>) cacheMap.computeIfAbsent(name, k -> {
            CacheConfig<K, V> cacheConfig = convertToCacheConfig(config);
            return createCache(name, cacheConfig);
        });
    }
    
    @Override
    public boolean removeCache(String name) {
        return cacheMap.remove(name) != null;
    }
    
    @Override
    public String getName() {
        return MANAGER_NAME;
    }
    
    /**
     * 创建缓存实例
     * 
     * @param name 缓存名称
     * @param config 缓存配置
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 缓存实例
     */
    protected <K, V> Cache<K, V> createCache(String name, CacheConfig<K, V> config) {
        // 根据缓存类型创建不同的缓存实现
        switch (config.getCacheType()) {
            case LOCAL:
                return createLocalCache(name, config);
            case REMOTE:
                return createRemoteCache(name, config);
            case BOTH:
                return createMultiLevelCache(name, config);
            default:
                throw new CacheException("不支持的缓存类型: " + config.getCacheType());
        }
    }
    
    /**
     * 创建本地缓存
     * 
     * @param name 缓存名称
     * @param config 缓存配置
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 本地缓存实例
     */
    protected <K, V> Cache<K, V> createLocalCache(String name, CacheConfig<K, V> config) {
        // 简单实现，默认使用CaffeineCache
        return new CaffeineCache<>(name, config);
    }
    
    /**
     * 创建远程缓存
     * 
     * @param name 缓存名称
     * @param config 缓存配置
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 远程缓存实例
     */
    protected <K, V> Cache<K, V> createRemoteCache(String name, CacheConfig<K, V> config) {
        // 这里应该创建RedisCache或其他远程缓存实现
        // 由于Redis缓存需要更多配置和依赖，这里暂时返回一个简单的LinkedHashMap缓存模拟
        return new LinkedHashMapCache<>(name, config);
    }
    
    /**
     * 创建多级缓存
     * 
     * @param name 缓存名称
     * @param config 缓存配置
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 多级缓存实例
     */
    protected <K, V> Cache<K, V> createMultiLevelCache(String name, CacheConfig<K, V> config) {
        // 这里应该创建组合了本地缓存和远程缓存的多级缓存
        // 由于多级缓存需要更复杂的逻辑，这里暂时返回一个本地缓存模拟
        return createLocalCache(name, config);
    }
    
    /**
     * 将QuickConfig转换为CacheConfig
     * 
     * @param quickConfig 快速配置
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 缓存配置
     */
    @SuppressWarnings("unchecked")
    protected <K, V> CacheConfig<K, V> convertToCacheConfig(QuickConfig quickConfig) {
        CacheConfig.Builder<K, V> builder = CacheConfig.newBuilder(quickConfig.getName());
        
        if (quickConfig.getExpire() != null) {
            builder.expireAfterWrite(quickConfig.getExpire().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        
        builder.cacheType(quickConfig.getCacheType())
               .localLimit(quickConfig.getLocalLimit())
               .syncLocal(quickConfig.isSyncLocal())
               .cacheNullValue(quickConfig.isCacheNullValue())
               .penetrationProtect(quickConfig.isPenetrationProtect());
        
        if (quickConfig.getLoader() != null) {
            builder.loader((Function<K, V>) quickConfig.getLoader());
        }
        
        if (quickConfig.getRefreshPolicy() != null) {
            builder.refreshPolicy(quickConfig.getRefreshPolicy());
        }
        
        // 设置序列化相关配置
        if (keyConvertor != null) {
            builder.keyConvertor(keyConvertor);
        }
        
        if (valueEncoder != null) {
            builder.valueEncoder(valueEncoder);
        }
        
        if (valueDecoder != null) {
            builder.valueDecoder(valueDecoder);
        }
        
        return builder.build();
    }
} 