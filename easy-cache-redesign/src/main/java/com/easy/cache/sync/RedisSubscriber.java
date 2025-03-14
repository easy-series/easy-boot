package com.easy.cache.sync;

import com.easy.cache.util.Serializer;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

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

    /**
     * Redis主题前缀
     */
    private static final String CHANNEL_PREFIX = "easy-cache:sync:";

    /**
     * Redis模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 序列化器
     */
    private final Serializer serializer;

    /**
     * 监听器映射表
     */
    private final Map<String, List<CacheEventListener>> listenerMap = new ConcurrentHashMap<>();

    /**
     * 事件处理线程池
     */
    private final ExecutorService eventProcessor;

    /**
     * Redis消息监听容器
     */
    private RedisMessageListenerContainer listenerContainer;

    /**
     * 是否已启动
     */
    private boolean running = false;

    /**
     * 构造函数
     *
     * @param redisTemplate Redis模板
     * @param serializer    序列化器
     */
    public RedisSubscriber(RedisTemplate<String, Object> redisTemplate, Serializer serializer) {
        this.redisTemplate = redisTemplate;
        this.serializer = serializer;
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

        // 创建监听容器
        listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(redisTemplate.getConnectionFactory());
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();

        // 添加消息监听器
        listenerContainer.addMessageListener(new CacheEventMessageListener(),
                new ChannelTopic(CHANNEL_PREFIX + "*"));
    }

    @Override
    public void shutdown() {
        running = false;
        if (listenerContainer != null) {
            listenerContainer.stop();
        }
        eventProcessor.shutdown();
    }

    /**
     * Redis消息监听器
     */
    private class CacheEventMessageListener implements MessageListener {

        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                // 反序列化事件
                CacheEvent event = (CacheEvent) redisTemplate.getValueSerializer().deserialize(message.getBody());
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