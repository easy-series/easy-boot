package com.easy.cache.spring;

import java.lang.reflect.Method;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.easy.cache.annotation.CacheInvalidate;
import com.easy.cache.annotation.CacheUpdate;
import com.easy.cache.annotation.Cached;

/**
 * 缓存注解Bean后处理器
 */
@Component
public class CacheAnnotationBeanPostProcessor implements BeanPostProcessor, InitializingBean, Ordered {

    @Autowired
    private CacheInterceptor cacheInterceptor;

    private Pointcut cachedPointcut;
    private Pointcut cacheUpdatePointcut;
    private Pointcut cacheInvalidatePointcut;

    private Advisor cachedAdvisor;
    private Advisor cacheUpdateAdvisor;
    private Advisor cacheInvalidateAdvisor;

    @Override
    public void afterPropertiesSet() {
        // 创建切点
        this.cachedPointcut = new AnnotationMatchingPointcut(Cached.class, true);
        this.cacheUpdatePointcut = new AnnotationMatchingPointcut(CacheUpdate.class, true);
        this.cacheInvalidatePointcut = new AnnotationMatchingPointcut(CacheInvalidate.class, true);

        // 创建通知器
        this.cachedAdvisor = new DefaultPointcutAdvisor(this.cachedPointcut, this.cacheInterceptor);
        this.cacheUpdateAdvisor = new DefaultPointcutAdvisor(this.cacheUpdatePointcut, this.cacheInterceptor);
        this.cacheInvalidateAdvisor = new DefaultPointcutAdvisor(this.cacheInvalidatePointcut, this.cacheInterceptor);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (hasCacheAnnotation(bean.getClass())) {
            // 创建代理
            return createProxy(bean);
        }
        return bean;
    }

    private boolean hasCacheAnnotation(Class<?> clazz) {
        // 检查类上的注解
        if (AnnotationUtils.findAnnotation(clazz, Cached.class) != null ||
                AnnotationUtils.findAnnotation(clazz, CacheUpdate.class) != null ||
                AnnotationUtils.findAnnotation(clazz, CacheInvalidate.class) != null) {
            return true;
        }

        // 检查方法上的注解
        for (Method method : clazz.getMethods()) {
            if (AnnotationUtils.findAnnotation(method, Cached.class) != null ||
                    AnnotationUtils.findAnnotation(method, CacheUpdate.class) != null ||
                    AnnotationUtils.findAnnotation(method, CacheInvalidate.class) != null) {
                return true;
            }
        }

        return false;
    }

    private Object createProxy(Object bean) {
        // 创建代理
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.addAdvisor(cachedAdvisor);
        proxyFactory.addAdvisor(cacheUpdateAdvisor);
        proxyFactory.addAdvisor(cacheInvalidateAdvisor);
        return proxyFactory.getProxy();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}