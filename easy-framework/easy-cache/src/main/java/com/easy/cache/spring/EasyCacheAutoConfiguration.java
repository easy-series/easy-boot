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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Easy Cache 自动配置类
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "easy.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(EasyCacheAspect.class)
public class EasyCacheAutoConfiguration {

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

    /**
     * 配置缓存管理器
     */
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
}