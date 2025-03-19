package com.easy.cache.spring;

import com.easy.cache.core.CacheManager;
import com.easy.cache.implementation.DefaultCacheManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 缓存管理器工厂Bean
 */
public class CacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean {
    
    private CacheManager cacheManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 创建默认缓存管理器
        this.cacheManager = new DefaultCacheManager();
    }

    @Override
    public CacheManager getObject() throws Exception {
        return this.cacheManager;
    }

    @Override
    public Class<?> getObjectType() {
        return CacheManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
} 