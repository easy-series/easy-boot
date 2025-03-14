package com.easy.cache.sync;

import com.easy.cache.core.RedisCache.Serializer;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于Redis的缓存事件发布者实现
 */
public class RedisPublisher implements CacheEventPublisher {

    private static final String CHANNEL_PREFIX = "easy-cache:sync:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Serializer serializer;
    private final ExecutorService executor;

    /**
     * 创建Redis发布者
     * 
     * @param redisTemplate Redis模板
     * @param serializer    序列化器
     */
    public RedisPublisher(RedisTemplate<String, Object> redisTemplate, Serializer serializer) {
        this.redisTemplate = redisTemplate;
        this.serializer = serializer;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "CacheEventPublisher");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void publish(CacheEvent event) {
        if (event == null) {
            return;
        }

        // 异步发布事件，避免阻塞缓存操作
        executor.execute(() -> {
            try {
                String channel = CHANNEL_PREFIX + event.getCacheName();
                byte[] message = serializer.serialize(event);
                redisTemplate.convertAndSend(channel, message);
            } catch (Exception e) {
                System.err.println("发布缓存事件失败: " + e.getMessage());
            }
        });
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}