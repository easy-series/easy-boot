package com.easy.mq.redis.config;

import com.easy.mq.redis.core.RedisMQTemplate;
import com.easy.mq.redis.core.interceptor.RedisMessageInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis测试配置类
 */
@Configuration
public class RedisTestConfiguration {

    /**
     * 创建用于测试的RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建RedisTemplate
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置连接工厂
        template.setConnectionFactory(factory);
        
        // 设置序列化方式
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        
        // 设置key和value的序列化
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        
        // 执行后置设置
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建RedisMQTemplate用于测试
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMQTemplate redisMQTemplate(RedisTemplate<String, Object> redisTemplate) {
        return new RedisMQTemplate(redisTemplate);
    }

    /**
     * 创建一个测试用的消息拦截器
     */
    @Bean
    public RedisMessageInterceptor testRedisMessageInterceptor() {
        return new TestRedisMessageInterceptor();
    }

    /**
     * 测试用的消息拦截器实现
     */
    public static class TestRedisMessageInterceptor implements RedisMessageInterceptor {
        @Override
        public void sendMessageBefore(com.easy.mq.redis.core.message.AbstractRedisMessage message) {
            message.addHeader("test-header", "test-value");
        }

        @Override
        public void sendMessageAfter(com.easy.mq.redis.core.message.AbstractRedisMessage message) {
            // 不需要实现
        }

        @Override
        public void consumeMessageBefore(com.easy.mq.redis.core.message.AbstractRedisMessage message) {
            // 不需要实现
        }

        @Override
        public void consumeMessageAfter(com.easy.mq.redis.core.message.AbstractRedisMessage message) {
            // 不需要实现
        }
    }
} 