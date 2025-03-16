package com.easy.lock;

import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import com.easy.lock.aop.LockInterceptor;
import com.easy.lock.core.Lock;
import com.easy.lock.core.RedisLock;
import com.easy.lock.core.executor.RedisLockExecutor;
import com.easy.lock.monitor.LockMonitor;
import com.easy.lock.template.LockTemplate;

/**
 * 分布式锁测试基类
 * 提供公共的测试配置
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BaseLockTest.TestApplication.class)
public abstract class BaseLockTest {

    /**
     * 测试用内部应用程序配置
     */
    @SpringBootApplication
    @ComponentScan("com.easy.lock")
    @EnableAspectJAutoProxy
    public static class TestApplication {

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName("localhost");
            config.setPort(6379);
            config.setDatabase(1);
            config.setPassword("123456");
            return new LettuceConnectionFactory(config);
        }

        @Bean
        public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
            return new StringRedisTemplate(redisConnectionFactory);
        }

        @Bean
        public RedisLockExecutor redisLockExecutor(StringRedisTemplate redisTemplate) {
            return new RedisLockExecutor(redisTemplate);
        }

        @Bean
        public RedisLock redisLock(RedisLockExecutor redisLockExecutor, LockMonitor lockMonitor) {
            RedisLock lock = new RedisLock(redisLockExecutor);
            lock.setLockMonitor(lockMonitor);
            return lock;
        }

        @Bean
        public LockTemplate lockTemplate(Lock lock) {
            return new LockTemplate(lock);
        }

        @Bean
        public LockInterceptor lockInterceptor(LockTemplate lockTemplate) {
            return new LockInterceptor(lockTemplate);
        }
    }
}