package com.easy.lock.config;

import com.easy.lock.aop.LockInterceptor;
import com.easy.lock.core.Lock;
import com.easy.lock.core.RedisLock;
import com.easy.lock.core.executor.RedisLockExecutor;
import com.easy.lock.monitor.LockMonitor;
import com.easy.lock.template.LockTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 分布式锁自动配置类
 */
@AutoConfiguration
@EnableConfigurationProperties(EasyLockProperties.class)
@ConditionalOnProperty(prefix = "easy.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EasyLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LockMonitor lockMonitor() {
        return new LockMonitor();
    }

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(RedisLockExecutor.class)
    public RedisLockExecutor redisLockExecutor(StringRedisTemplate redisTemplate) {
        return new RedisLockExecutor(redisTemplate);
    }

    @Bean
    @ConditionalOnBean(RedisLockExecutor.class)
    @ConditionalOnMissingBean(RedisLock.class)
    public RedisLock redisLock(RedisLockExecutor redisLockExecutor) {
        return new RedisLock(redisLockExecutor);
    }

    @Bean
    @ConditionalOnBean(Lock.class)
    @ConditionalOnMissingBean(LockTemplate.class)
    public LockTemplate lockTemplate(Lock lock) {
        return new LockTemplate(lock);
    }

    @Bean
    @ConditionalOnBean(LockTemplate.class)
    @ConditionalOnMissingBean(LockInterceptor.class)
    public LockInterceptor lockInterceptor(LockTemplate lockTemplate) {
        return new LockInterceptor(lockTemplate);
    }
}