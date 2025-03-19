package com.easy.cache.spring;

import java.lang.reflect.Method;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.easy.cache.annotation.CacheRefresh;

/**
 * 缓存刷新任务注册器
 */
@Component
public class CacheRefreshTaskRegistrar implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CacheRefreshTask refreshTask;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 获取所有Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);

            // 获取Bean的所有方法
            Method[] methods = bean.getClass().getMethods();
            for (Method method : methods) {
                // 检查是否有@CacheRefresh注解
                CacheRefresh refresh = method.getAnnotation(CacheRefresh.class);
                if (refresh != null) {
                    // 注册刷新方法
                    refreshTask.registerRefreshMethod(refresh.name(), method);
                }
            }
        }
    }
}