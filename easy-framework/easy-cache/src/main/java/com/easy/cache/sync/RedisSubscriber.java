package com.easy.cache.sync;

import com.easy.cache.core.RedisCache.Serializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于Redis的缓存事件订阅者实现
 */
public class RedisSubscriber implements CacheEventSubscriber {

    private static final String CHANNEL_PREFIX = "easy-cache:sync:";

    private final JedisPool jedisPool;
    private final Serializer serializer;
    private final Map<String, List<CacheEventListener>> listenerMap = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final ExecutorService eventProcessor;
    private volatile boolean running = false;

    /**
     * 创建Redis订阅者
     * 
     * @param jedisPool  Redis连接池
     * @param serializer 序列化器
     */
    public RedisSubscriber(JedisPool jedisPool, Serializer serializer) {
        this.jedisPool = jedisPool;
        this.serializer = serializer;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "CacheEventSubscriber");
            thread.setDaemon(true);
            return thread;
        });
        this.eventProcessor = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, "CacheEventProcessor");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void subscribe(CacheEventListener listener) {
        if (listener == null) {
            return;
        }

        // 为所有缓存注册监听器
        listenerMap.computeIfAbsent("*", k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * 为指定缓存注册监听器
     * 
     * @param cacheName 缓存名称
     * @param listener  监听器
     */
    public void subscribe(String cacheName, CacheEventListener listener) {
        if (cacheName == null || listener == null) {
            return;
        }

        listenerMap.computeIfAbsent(cacheName, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unsubscribe(CacheEventListener listener) {
        if (listener == null) {
            return;
        }

        listenerMap.values().forEach(listeners -> listeners.remove(listener));
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        running = true;
        executor.execute(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                // 订阅所有缓存通道
                jedis.psubscribe(new CacheEventJedisPubSub(), CHANNEL_PREFIX + "*");
            } catch (Exception e) {
                System.err.println("缓存事件订阅失败: " + e.getMessage());
                running = false;
            }
        });
    }

    @Override
    public void shutdown() {
        running = false;
        executor.shutdown();
        eventProcessor.shutdown();
    }

    /**
     * Redis发布/订阅处理器
     */
    private class CacheEventJedisPubSub extends JedisPubSub {

        @Override
        public void onPMessage(String pattern, String channel, String message) {
            try {
                // 反序列化事件
                CacheEvent event = serializer.deserialize(message.getBytes(), CacheEvent.class);
                if (event == null) {
                    return;
                }

                // 异步处理事件
                eventProcessor.execute(() -> {
                    // 通知特定缓存的监听器
                    List<CacheEventListener> cacheListeners = listenerMap.get(event.getCacheName());
                    if (cacheListeners != null) {
                        for (CacheEventListener listener : cacheListeners) {
                            try {
                                listener.onEvent(event);
                            } catch (Exception e) {
                                System.err.println("处理缓存事件失败: " + e.getMessage());
                            }
                        }
                    }

                    // 通知全局监听器
                    List<CacheEventListener> globalListeners = listenerMap.get("*");
                    if (globalListeners != null) {
                        for (CacheEventListener listener : globalListeners) {
                            try {
                                listener.onEvent(event);
                            } catch (Exception e) {
                                System.err.println("处理缓存事件失败: " + e.getMessage());
                            }
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("解析缓存事件失败: " + e.getMessage());
            }
        }
    }
}