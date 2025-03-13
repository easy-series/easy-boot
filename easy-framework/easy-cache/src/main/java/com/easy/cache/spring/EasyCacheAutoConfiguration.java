package com.easy.cache.spring;

import com.easy.cache.core.CacheManager;
import com.easy.cache.core.RedisCache.Serializer;
import com.easy.cache.support.JdkSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Easy Cache 自动配置类
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "easy.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(EasyCacheAspect.class)
public class EasyCacheAutoConfiguration {

    /**
     * 配置缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(CacheProperties properties) {
        CacheManager cacheManager = CacheManager.getInstance();

        // 配置本地缓存
        if (properties.getLocal().isEnabled()) {
            cacheManager.setLocalCacheMaximumSize(properties.getLocal().getMaximumSize());
            cacheManager.setLocalCacheInitialCapacity(properties.getLocal().getInitialCapacity());
            cacheManager.setDefaultLocalExpiration(properties.getLocal().getExpireAfterWrite());
            cacheManager.setDefaultLocalTimeUnit(properties.getLocal().getTimeUnit());
        }

        // 配置Redis缓存
        if (properties.getRedis().isEnabled()) {
            // 配置Redis连接池
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(properties.getRedis().getMaxTotal());
            poolConfig.setMaxIdle(properties.getRedis().getMaxIdle());
            poolConfig.setMinIdle(properties.getRedis().getMinIdle());

            // 创建Redis连接池
            JedisPool jedisPool = new JedisPool(
                    poolConfig,
                    properties.getRedis().getHost(),
                    properties.getRedis().getPort(),
                    properties.getRedis().getTimeout(),
                    properties.getRedis().getPassword(),
                    properties.getRedis().getDatabase());

            // 设置序列化器
            Serializer serializer;
            if ("JSON".equalsIgnoreCase(properties.getRedis().getSerializer())) {
                try {
                    // 尝试创建FastJson序列化器
                    Class<?> clazz = Class.forName("com.easy.cache.support.FastJsonSerializer");
                    serializer = (Serializer) clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    // 如果FastJson不可用，使用JDK序列化器
                    serializer = new JdkSerializer();
                }
            } else {
                serializer = new JdkSerializer();
            }

            // 配置缓存管理器
            cacheManager.setJedisPool(jedisPool);
            cacheManager.setSerializer(serializer);
            cacheManager.setDefaultRedisExpiration(properties.getRedis().getExpireAfterWrite());
            cacheManager.setDefaultRedisTimeUnit(properties.getRedis().getTimeUnit());
        }

        // 配置多级缓存
        if (properties.getMultiLevel().isEnabled()) {
            cacheManager.setWriteThrough(properties.getMultiLevel().isWriteThrough());
            cacheManager.setAsyncWrite(properties.getMultiLevel().isAsyncWrite());
        }

        return cacheManager;
    }
}