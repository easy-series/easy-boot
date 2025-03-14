package com.easy.easylock.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.easy.easylock.aop.LockInterceptor;
import com.easy.easylock.core.LockManager;
import com.easy.easylock.core.factory.LockFactory;
import com.easy.easylock.core.factory.LockFactory.LockType;
import com.easy.easylock.core.impl.RedisLockExecutor;
import com.easy.easylock.template.LockTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Easy Lock自动配置类
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(EasyLockProperties.class)
@ConditionalOnProperty(prefix = "easy.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EasyLockAutoConfiguration {

    /**
     * Redis锁执行器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisLockExecutor redisLockExecutor(StringRedisTemplate redisTemplate, EasyLockProperties properties) {
        RedisLockExecutor executor = new RedisLockExecutor(redisTemplate, properties.getKeyPrefix());
        return executor;
    }

    /**
     * 锁工厂Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedisLockExecutor.class)
    public LockFactory lockFactory(RedisLockExecutor redisLockExecutor) {
        LockFactory factory = new LockFactory(redisLockExecutor, LockType.REDIS);
        return factory;
    }

    /**
     * 锁管理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(LockFactory.class)
    public LockManager lockManager(LockFactory lockFactory) {
        return new LockManager(lockFactory);
    }

    /**
     * 锁拦截器Bean，用于处理@EasyLock注解
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(LockManager.class)
    public LockInterceptor lockInterceptor(LockManager lockManager) {
        return new LockInterceptor(lockManager);
    }

    /**
     * 锁模板Bean，提供手动加锁的方式
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(LockManager.class)
    public LockTemplate lockTemplate(LockManager lockManager) {
        log.info("创建 LockTemplate Bean");
        return new LockTemplate(lockManager);
    }
}