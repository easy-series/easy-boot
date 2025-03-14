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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 缓存自动配置类，简化Spring Boot集成
 */
@Configuration
@ConditionalOnClass(CacheManager.class)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {
    
    /**
     * 配置Redis连接工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redisConnectionFactory(CacheProperties properties) {
        if (!properties.getRedis().isEnabled()) {
            return null;
        }
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getRedis().getHost());
        config.setPort(properties.getRedis().getPort());
        config.setPassword(properties.getRedis().getPassword());
        config.setDatabase(properties.getRedis().getDatabase());
        
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(config);
        return connectionFactory;
    }
    
    /**
     * 配置Redis模板
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            return null;
        }
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JdkSerializationRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(CacheProperties properties, RedisTemplate<String, Object> redisTemplate) {
        CacheManager cacheManager = CacheManager.getInstance();
        
        // 配置本地缓存
        if (properties.getLocal().isEnabled()) {
            cacheManager.setLocalCacheMaximumSize(properties.getLocal().getMaximumSize());
            cacheManager.setLocalCacheInitialCapacity(properties.getLocal().getInitialCapacity());
            cacheManager.setDefaultLocalExpiration(properties.getLocal().getExpireAfterWrite());
            cacheManager.setDefaultLocalTimeUnit(properties.getLocal().getTimeUnit());
        }
        
        // 配置Redis缓存
        if (properties.getRedis().isEnabled() && redisTemplate != null) {
            // 配置序列化器
            Serializer serializer;
            if ("JSON".equalsIgnoreCase(properties.getRedis().getSerializer())) {
                serializer = new JsonSerializer();
            } else {
                serializer = new JdkSerializer();
            }
            
            // 配置缓存管理器
            cacheManager.setRedisTemplate(redisTemplate);
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