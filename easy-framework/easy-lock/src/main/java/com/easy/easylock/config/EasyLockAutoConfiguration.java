package com.easy.easylock.config;

import com.easy.easylock.aop.LockInterceptor;
import com.easy.easylock.core.LockExecutor;
import com.easy.easylock.core.impl.RedisLockExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 分布式锁自动配置
 */
@AutoConfiguration
public class EasyLockAutoConfiguration {

    /**
     * 创建Redis锁执行器
     */
    @Bean
    @ConditionalOnMissingBean(LockExecutor.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    public LockExecutor lockExecutor(StringRedisTemplate redisTemplate) {
        return new RedisLockExecutor(redisTemplate);
    }

    /**
     * 创建锁拦截器
     */
    @Bean
    @ConditionalOnBean(LockExecutor.class)
    public LockInterceptor lockInterceptor(LockExecutor lockExecutor) {
        return new LockInterceptor(lockExecutor);
    }
}