package com.easy.cache.spring;

import com.easy.cache.annotation.CacheInterceptor;
import com.easy.cache.annotation.Cached;
import com.easy.cache.core.CacheManager;
import com.easy.cache.support.DefaultKeyGenerator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 缓存注解切面
 */
@Aspect
@Component
public class EasyCacheAspect {

    private final CacheManager cacheManager;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final CacheInterceptor cacheInterceptor;

    @Autowired
    public EasyCacheAspect(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.cacheInterceptor = new CacheInterceptor(new DefaultKeyGenerator(), cacheManager);
    }

    /**
     * 定义切点
     */
    @Pointcut("@annotation(com.easy.cache.annotation.Cached)")
    public void cachedPointcut() {
    }

    /**
     * 环绕通知
     */
    @Around("cachedPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Cached cached = method.getAnnotation(Cached.class);

        if (cached == null) {
            return joinPoint.proceed();
        }

        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();

        return cacheInterceptor.process(target, method, args, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 解析SpEL表达式
     */
    public String parseKey(String keyExpression, Method method, Object[] args) {
        if (!keyExpression.contains("#")) {
            return keyExpression;
        }

        EvaluationContext context = new MethodBasedEvaluationContext(null, method, args, parameterNameDiscoverer);
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
            context.setVariable("arg" + i, args[i]);
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}