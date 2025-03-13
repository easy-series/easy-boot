package com.easy.cache.sync;

import com.easy.cache.core.RedisCache.Serializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于Redis的缓存事件发布者实现
 */
public class RedisPublisher implements CacheEventPublisher {

    private static final String CHANNEL_PREFIX = "easy-cache:sync:";

    private final JedisPool jedisPool;
    private final Serializer serializer;
    private final ExecutorService executor;

    /**
     * 创建Redis发布者
     * 
     * @param jedisPool  Redis连接池
     * @param serializer 序列化器
     */
    public RedisPublisher(JedisPool jedisPool, Serializer serializer) {
        this.jedisPool = jedisPool;
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
            try (Jedis jedis = jedisPool.getResource()) {
                byte[] channel = (CHANNEL_PREFIX + event.getCacheName()).getBytes();
                byte[] message = serializer.serialize(event);
                jedis.publish(channel, message);
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