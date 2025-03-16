package com.easy.cache.autoconfigure;

import com.easy.cache.api.CacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.easy.cache.core.DefaultCacheManager;
import com.easy.cache.support.convertor.FastJsonKeyConvertor;
import com.easy.cache.support.serializer.FastJsonSerializer;
import com.easy.cache.support.sync.CacheEventPublisher;
import com.easy.cache.support.sync.CacheEventSubscriber;
import com.easy.cache.support.sync.DefaultLocalCacheSyncManager;
import com.easy.cache.support.sync.LocalCacheSyncManager;
import com.easy.cache.support.sync.redis.RedisCacheEventPublisher;
import com.easy.cache.support.sync.redis.RedisCacheEventSubscriber;
import com.easy.cache.template.CacheTemplate;

/**
 * 缓存自动配置类
 */
@Configuration
@EnableConfigurationProperties(EasyCacheProperties.class)
public class EasyCacheAutoConfiguration {

    /**
     * 创建FastJson键转换器
     */
    @Bean
    @ConditionalOnMissingBean
    public FastJsonKeyConvertor keyConvertor() {
        return new FastJsonKeyConvertor();
    }

    /**
     * 创建FastJson序列化器
     */
    @Bean
    @ConditionalOnMissingBean
    public FastJsonSerializer serializer() {
        return new FastJsonSerializer();
    }

    /**
     * 创建Redis模板
     */
    @Bean
    @ConditionalOnMissingBean(name = "cacheRedisTemplate")
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, byte[]> cacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建Redis消息监听容器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "easy.cache.sync-channel")
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    /**
     * 创建本地缓存同步管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalCacheSyncManager localCacheSyncManager() {
        return new DefaultLocalCacheSyncManager();
    }

    /**
     * 创建Redis缓存事件发布器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnProperty(name = "easy.cache.sync-channel")
    public CacheEventPublisher cacheEventPublisher(StringRedisTemplate redisTemplate, EasyCacheProperties properties) {
        return new RedisCacheEventPublisher(redisTemplate, properties.getSyncChannel());
    }

    /**
     * 创建Redis缓存事件订阅器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ RedisMessageListenerContainer.class, LocalCacheSyncManager.class })
    @ConditionalOnProperty(name = "easy.cache.sync-channel")
    public CacheEventSubscriber cacheEventSubscriber(RedisMessageListenerContainer listenerContainer,
            LocalCacheSyncManager syncManager,
            EasyCacheProperties properties) {
        RedisCacheEventSubscriber subscriber = new RedisCacheEventSubscriber(
                listenerContainer, syncManager, properties.getSyncChannel());
        subscriber.subscribe();
        return subscriber;
    }

    /**
     * 创建缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultCacheManager cacheManager(FastJsonKeyConvertor keyConvertor,
            FastJsonSerializer serializer,
            RedisTemplate<String, byte[]> cacheRedisTemplate,
            CacheEventPublisher eventPublisher) {
        return new DefaultCacheManager(keyConvertor, serializer, cacheRedisTemplate, eventPublisher);
    }

    /**
     * 创建缓存模板
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheTemplate cacheTemplate(CacheManager cacheManager) {
        return new CacheTemplate(cacheManager);
    }
}