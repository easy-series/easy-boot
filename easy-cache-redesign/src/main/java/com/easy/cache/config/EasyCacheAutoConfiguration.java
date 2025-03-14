package com.easy.cache.config;

import com.easy.cache.core.*;
import com.easy.cache.sync.CacheSyncManager;
import com.easy.cache.util.JdkSerializer;
import com.easy.cache.util.JsonSerializer;
import com.easy.cache.util.Serializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 缓存自动配置类
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "easy.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EasyCacheAutoConfiguration {

    private final CacheProperties cacheProperties;

    public EasyCacheAutoConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * 配置JDK序列化器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "easy.cache", name = "serializer", havingValue = "jdk", matchIfMissing = true)
    public Serializer jdkSerializer() {
        return new JdkSerializer();
    }

    /**
     * 配置JSON序列化器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "easy.cache", name = "serializer", havingValue = "json")
    public Serializer jsonSerializer() {
        return new JsonSerializer();
    }

    /**
     * 初始化缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(Serializer serializer) {
        // 获取单例实例
        CacheManager cacheManager = CacheManager.getInstance();

        // 设置序列化器
        cacheManager.setSerializer(serializer);

        // 设置默认本地缓存配置
        CacheProperties.LocalCacheProperties localProps = cacheProperties.getLocal();
        cacheManager.setDefaultLocalCacheConfig(
                localProps.getMaxSize(),
                localProps.getExpireAfterWrite(),
                localProps.getRefreshAfterWrite());

        // 设置默认Redis缓存配置
        CacheProperties.RedisCacheProperties redisProps = cacheProperties.getRedis();
        cacheManager.setDefaultRedisCacheConfig(
                redisProps.getKeyPrefix(),
                redisProps.getExpireAfterWrite());

        return cacheManager;
    }

    /**
     * 初始化缓存同步管理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "easy.cache.sync", name = "enabled", havingValue = "true")
    public CacheSyncManager cacheSyncManager(RedisTemplate<String, Object> redisTemplate, Serializer serializer) {
        // 获取单例实例
        CacheSyncManager syncManager = CacheSyncManager.getInstance();

        // 设置同步策略
        CacheProperties.SyncProperties syncProps = cacheProperties.getSync();
        if ("update".equalsIgnoreCase(syncProps.getStrategy())) {
            syncManager.setDefaultSyncStrategy(CacheSyncManager.SyncStrategy.UPDATE);
        } else {
            syncManager.setDefaultSyncStrategy(CacheSyncManager.SyncStrategy.INVALIDATE);
        }

        // 初始化同步管理器
        syncManager.init(redisTemplate, serializer);

        return syncManager;
    }

    /**
     * 配置自定义缓存
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheInitializer cacheInitializer(CacheManager cacheManager, RedisTemplate<String, Object> redisTemplate) {
        return new CacheInitializer(cacheManager, redisTemplate, cacheProperties);
    }

    /**
     * 缓存初始化器，负责创建配置文件中定义的缓存
     */
    public static class CacheInitializer {

        private final CacheManager cacheManager;
        private final RedisTemplate<String, Object> redisTemplate;
        private final CacheProperties cacheProperties;

        public CacheInitializer(CacheManager cacheManager, RedisTemplate<String, Object> redisTemplate,
                CacheProperties cacheProperties) {
            this.cacheManager = cacheManager;
            this.redisTemplate = redisTemplate;
            this.cacheProperties = cacheProperties;

            // 设置Redis模板
            if (redisTemplate != null) {
                cacheManager.setRedisTemplate(redisTemplate);
            }

            // 初始化自定义缓存
            initCustomCaches();

            // 启用缓存同步
            enableCacheSync();
        }

        /**
         * 初始化自定义缓存
         */
        private void initCustomCaches() {
            // 获取自定义缓存配置
            cacheProperties.getCaches().forEach((name, props) -> {
                String type = props.getType() != null ? props.getType() : cacheProperties.getType();

                try {
                    if ("local".equalsIgnoreCase(type)) {
                        // 创建本地缓存
                        initLocalCache(name, props);
                    } else if ("redis".equalsIgnoreCase(type)) {
                        // 创建Redis缓存
                        initRedisCache(name, props);
                    } else if ("multilevel".equalsIgnoreCase(type)) {
                        // 创建多级缓存
                        initMultiLevelCache(name, props);
                    }
                } catch (Exception e) {
                    System.err.println("初始化缓存失败: " + name + ", " + e.getMessage());
                }
            });
        }

        /**
         * 初始化本地缓存
         */
        private void initLocalCache(String name, CacheProperties.CustomCacheProperties props) {
            // 获取缓存配置
            int maxSize = props.getMaxSize() != null ? props.getMaxSize() : cacheProperties.getLocal().getMaxSize();
            int expireAfterWrite = props.getExpireAfterWrite() != null ? props.getExpireAfterWrite()
                    : cacheProperties.getLocal().getExpireAfterWrite();
            int refreshAfterWrite = props.getRefreshAfterWrite() != null ? props.getRefreshAfterWrite()
                    : cacheProperties.getLocal().getRefreshAfterWrite();

            // 创建本地缓存
            cacheManager.getOrCreateLocalCache(name, maxSize, expireAfterWrite, refreshAfterWrite);
            System.out.println("已创建本地缓存: " + name);
        }

        /**
         * 初始化Redis缓存
         */
        private void initRedisCache(String name, CacheProperties.CustomCacheProperties props) {
            // 获取缓存配置
            String keyPrefix = props.getKeyPrefix() != null ? props.getKeyPrefix()
                    : cacheProperties.getRedis().getKeyPrefix();
            int expireAfterWrite = props.getExpireAfterWrite() != null ? props.getExpireAfterWrite()
                    : cacheProperties.getRedis().getExpireAfterWrite();

            // 创建Redis缓存
            cacheManager.getOrCreateRedisCache(name, keyPrefix, expireAfterWrite);
            System.out.println("已创建Redis缓存: " + name);
        }

        /**
         * 初始化多级缓存
         */
        private void initMultiLevelCache(String name, CacheProperties.CustomCacheProperties props) {
            // 首先创建本地缓存
            String localCacheName = name + ":local";
            initLocalCache(localCacheName, props);

            // 然后创建Redis缓存
            String redisCacheName = name + ":redis";
            initRedisCache(redisCacheName, props);

            // 最后创建多级缓存
            cacheManager.getOrCreateMultiLevelCache(name, localCacheName, redisCacheName);
            System.out.println("已创建多级缓存: " + name);
        }

        /**
         * 启用缓存同步
         */
        private void enableCacheSync() {
            // 检查是否启用缓存同步
            if (!cacheProperties.getSync().isEnabled() || redisTemplate == null) {
                return;
            }

            // 获取同步管理器
            CacheSyncManager syncManager = CacheSyncManager.getInstance();

            // 获取同步策略
            CacheSyncManager.SyncStrategy strategy = "update".equalsIgnoreCase(cacheProperties.getSync().getStrategy())
                    ? CacheSyncManager.SyncStrategy.UPDATE
                    : CacheSyncManager.SyncStrategy.INVALIDATE;

            // 为每个缓存启用同步
            cacheProperties.getCaches().forEach((name, props) -> {
                Boolean syncEnabled = props.getSyncEnabled();
                if (syncEnabled == null || syncEnabled) {
                    // 获取自定义同步策略
                    CacheSyncManager.SyncStrategy customStrategy = strategy;
                    if (props.getSyncStrategy() != null) {
                        customStrategy = "update".equalsIgnoreCase(props.getSyncStrategy())
                                ? CacheSyncManager.SyncStrategy.UPDATE
                                : CacheSyncManager.SyncStrategy.INVALIDATE;
                    }

                    // 启用缓存同步
                    syncManager.enableSync(name, true, customStrategy);
                }
            });
        }
    }
}