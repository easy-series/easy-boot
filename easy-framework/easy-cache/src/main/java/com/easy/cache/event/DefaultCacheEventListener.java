package com.easy.cache.event;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import org.springframework.data.redis.serialization.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serialization.RedisSerializer;

/**
 * 默认的缓存事件监听器实现
 */
public class DefaultCacheEventListener implements CacheEventListener {

    /**
     * 缓存管理器
     */
    private final CacheManager cacheManager;
    
    /**
     * 序列化器
     */
    private final RedisSerializer<Object> serializer;
    
    /**
     * 当前实例ID
     */
    private final String instanceId;
    
    /**
     * 构造函数
     *
     * @param cacheManager 缓存管理器
     */
    public DefaultCacheEventListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.serializer = new JdkSerializationRedisSerializer();
        this.instanceId = java.util.UUID.randomUUID().toString();
    }

    @Override
    public void onEvent(CacheEvent event) {
        // 如果事件来自当前实例，则忽略（防止循环）
        if (instanceId.equals(event.getEventId())) {
            return;
        }
        
        // 获取缓存
        Cache<Object, Object> cache = cacheManager.getCache(event.getCacheName());
        if (cache == null) {
            return;
        }
        
        // 根据事件类型处理
        switch (event.getEventType()) {
            case PUT:
                Object value = event.getValue();
                // 如果值是序列化形式，需要反序列化
                if (value instanceof byte[]) {
                    value = serializer.deserialize((byte[]) value);
                }
                cache.put(event.getKey(), value);
                break;
                
            case REMOVE:
                cache.remove(event.getKey());
                break;
                
            case CLEAR:
                cache.clear();
                break;
                
            default:
                break;
        }
    }
} 