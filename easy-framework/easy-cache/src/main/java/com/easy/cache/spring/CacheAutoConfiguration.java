package com.easy.cache.spring;

import com.easy.cache.aop.CacheAspect;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.RedisCache.Serializer;
import com.easy.cache.support.JdkSerializer;
import com.easy.cache.support.JsonSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 缓存自动配置类，简化Spring Boot集成
 */
@Configuration
@ConditionalOnClass(CacheManager.class)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {
    
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
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(properties.getRedis().getMaxTotal());
            poolConfig.setMaxIdle(properties.getRedis().getMaxIdle());
            poolConfig.setMinIdle(properties.getRedis().getMinIdle());
            
            JedisPool jedisPool = new JedisPool(
                poolConfig,
                properties.getRedis().getHost(),
                properties.getRedis().getPort(),
                properties.getRedis().getTimeout(),
                properties.getRedis().getPassword(),
                properties.getRedis().getDatabase()
            );
            
            cacheManager.setJedisPool(jedisPool);
            
            // 配置序列化器
            Serializer serializer;
            if ("JSON".equalsIgnoreCase(properties.getRedis().getSerializer())) {
                serializer = new JsonSerializer();
            } else {
                serializer = new JdkSerializer();
            }
            
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
    
    @Bean
    @ConditionalOnMissingBean
    public CacheAspect cacheAspect(CacheManager cacheManager) {
        return new CacheAspect(cacheManager);
    }
} 