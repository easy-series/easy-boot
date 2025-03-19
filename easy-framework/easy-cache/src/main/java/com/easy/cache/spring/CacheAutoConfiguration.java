package com.easy.cache.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.easy.cache.core.CacheManager;
import com.easy.cache.implementation.DefaultCacheManager;

/**
 * 缓存自动配置
 */
@Configuration
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        return new DefaultCacheManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheAnnotationBeanPostProcessor cacheAnnotationBeanPostProcessor() {
        return new CacheAnnotationBeanPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheInterceptor cacheInterceptor() {
        return new CacheInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultCachingConfigurer defaultCachingConfigurer() {
        return new DefaultCachingConfigurer();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManagerFactoryBean cacheManagerFactoryBean() {
        return new CacheManagerFactoryBean();
    }
}