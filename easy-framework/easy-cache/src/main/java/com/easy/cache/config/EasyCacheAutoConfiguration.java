package com.easy.cache.config;

import com.easy.cache.annotation.CacheAspect;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.DefaultKeyGenerator;
import com.easy.cache.core.KeyGenerator;
import com.easy.cache.core.SpELParser;
import com.easy.cache.core.SpelKeyGenerator;
import com.easy.cache.event.CacheEventListener;
import com.easy.cache.event.CacheSyncManager;
import com.easy.cache.event.DefaultCacheEventListener;
import com.easy.cache.event.redis.RedisPublisher;
import com.easy.cache.event.redis.RedisSubscriber;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Easy Cache自动配置类
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class EasyCacheAutoConfiguration {

    /**
     * 配置缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(CacheProperties cacheProperties, RedisTemplate<String, Object> redisTemplate) {
        return new CacheManager(cacheProperties, redisTemplate);
    }

    /**
     * 配置默认的键生成器
     */
    @Bean
    @ConditionalOnMissingBean(KeyGenerator.class)
    public KeyGenerator keyGenerator() {
        return new DefaultKeyGenerator();
    }

    /**
     * 配置SpEL解析器
     */
    @Bean
    @ConditionalOnMissingBean(SpELParser.class)
    public SpELParser spELParser() {
        return new SpELParser();
    }

    /**
     * 配置基于SpEL表达式的键生成器
     */
    @Bean
    @ConditionalOnMissingBean(SpelKeyGenerator.class)
    public SpelKeyGenerator spelKeyGenerator(SpELParser spELParser) {
        return new SpelKeyGenerator(spELParser);
    }

    /**
     * 配置缓存切面
     */
    @Bean
    @ConditionalOnMissingBean(CacheAspect.class)
    public CacheAspect cacheAspect(CacheManager cacheManager, SpelKeyGenerator spelKeyGenerator) {
        return new CacheAspect(cacheManager, spelKeyGenerator);
    }
    
    /**
     * Redis事件发布器配置
     */
    @Bean
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "easy.cache", name = "sync.enabled", havingValue = "true")
    public RedisPublisher redisPublisher(RedisTemplate<String, Object> redisTemplate, CacheProperties cacheProperties) {
        return new RedisPublisher(redisTemplate, cacheProperties.getSync().getChannelName());
    }
    
    /**
     * Redis事件订阅器配置
     */
    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "easy.cache", name = "sync.enabled", havingValue = "true")
    public RedisSubscriber redisSubscriber(RedisConnectionFactory connectionFactory, CacheProperties cacheProperties) {
        return new RedisSubscriber(connectionFactory, cacheProperties.getSync().getChannelName());
    }
    
    /**
     * 默认缓存事件监听器配置
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "easy.cache", name = "sync.enabled", havingValue = "true")
    public CacheEventListener defaultCacheEventListener(CacheManager cacheManager) {
        return new DefaultCacheEventListener(cacheManager);
    }
    
    /**
     * 缓存同步管理器配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "easy.cache", name = "sync.enabled", havingValue = "true")
    public CacheSyncManager cacheSyncManager(RedisPublisher publisher, RedisSubscriber subscriber, 
                                          CacheEventListener defaultCacheEventListener) {
        CacheSyncManager syncManager = new CacheSyncManager(publisher, subscriber);
        syncManager.addListener(defaultCacheEventListener);
        if (Boolean.TRUE.equals(syncManager.isEnabled())) {
            syncManager.enable();
        }
        return syncManager;
    }
} 