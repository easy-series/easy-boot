package com.easy.cache.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 缓存刷新任务配置
 */
@Configuration
public class CacheRefreshTaskConfiguration {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("cache-refresh-");
        scheduler.setErrorHandler(t -> {
            // 记录错误日志
            t.printStackTrace();
        });
        return scheduler;
    }
}