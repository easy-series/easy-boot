package com.easy.cache.config;

import com.easy.cache.annotation.EnableCaching;
import com.easy.cache.aop.CacheAspect;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.RedisCache.Serializer;
import com.easy.cache.support.JdkSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import redis.clients.jedis.JedisPool;

/**
 * 缓存配置类，用于自动配置缓存
 */
@Configuration
public class CacheConfiguration implements ImportAware {

    private AnnotationAttributes enableCaching;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableCaching = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableCaching.class.getName(), false));
    }

    @Bean
    public CacheManager cacheManager(@Autowired(required = false) JedisPool jedisPool,
            @Autowired(required = false) Serializer serializer) {
        CacheManager cacheManager = CacheManager.getInstance();

        // 如果启用了远程缓存，则设置JedisPool和序列化器
        if (enableCaching != null && enableCaching.getBoolean("enableRemoteCache")) {
            if (jedisPool == null) {
                throw new IllegalStateException("JedisPool is required when enableRemoteCache is true");
            }
            if (serializer == null) {
                serializer = new JdkSerializer();
            }
            cacheManager.setJedisPool(jedisPool);
            cacheManager.setSerializer(serializer);

            // 如果启用了缓存同步，则初始化缓存同步
            if (enableCaching.getBoolean("enableCacheSync")) {
                cacheManager.initCacheSync();
            }
        }

        return cacheManager;
    }

    @Bean
    public CacheAspect cacheAspect(CacheManager cacheManager) {
        return new CacheAspect(cacheManager);
    }

    @Bean
    public Serializer serializer() {
        return new JdkSerializer();
    }
}