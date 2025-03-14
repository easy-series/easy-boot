package com.easy.cache.event.redis;

import com.easy.cache.event.CacheEvent;
import com.easy.cache.event.CacheEventListener;
import com.easy.cache.event.CacheEventSubscriber;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serialization.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serialization.RedisSerializer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于Redis的事件订阅器实现
 */
public class RedisSubscriber implements CacheEventSubscriber {

    /**
     * Redis连接工厂
     */
    private final RedisConnectionFactory connectionFactory;
    
    /**
     * Redis通道名称
     */
    private final String channelName;
    
    /**
     * 序列化器
     */
    private final RedisSerializer<Object> serializer;
    
    /**
     * 事件监听器列表
     */
    private final List<CacheEventListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Redis消息监听容器
     */
    private RedisMessageListenerContainer container;
    
    /**
     * Redis消息监听器
     */
    private MessageListener messageListener;
    
    /**
     * 构造函数
     *
     * @param connectionFactory Redis连接工厂
     * @param channelName Redis通道名称
     */
    public RedisSubscriber(RedisConnectionFactory connectionFactory, String channelName) {
        this.connectionFactory = connectionFactory;
        this.channelName = channelName;
        this.serializer = new JdkSerializationRedisSerializer();
    }
    
    /**
     * 默认构造函数，使用默认通道名称
     *
     * @param connectionFactory Redis连接工厂
     */
    public RedisSubscriber(RedisConnectionFactory connectionFactory) {
        this(connectionFactory, "easy:cache:sync:channel");
    }

    @Override
    public void subscribe(CacheEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unsubscribe(CacheEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void start() {
        if (container != null) {
            return;
        }
        
        // 创建消息监听器
        messageListener = (message, pattern) -> {
            try {
                Object deserializedMessage = serializer.deserialize(message.getBody());
                if (deserializedMessage instanceof CacheEvent) {
                    CacheEvent event = (CacheEvent) deserializedMessage;
                    
                    // 分发事件到所有监听器
                    for (CacheEventListener listener : listeners) {
                        if (listener.supports(event)) {
                            try {
                                listener.onEvent(event);
                            } catch (Exception e) {
                                // 处理事件失败，记录日志
                                System.err.println("Failed to process cache event: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 反序列化失败，记录日志
                System.err.println("Failed to deserialize cache event: " + e.getMessage());
            }
        };
        
        // 创建并启动消息监听容器
        container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListener, new ChannelTopic(channelName));
        container.afterPropertiesSet();
        container.start();
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
            container = null;
        }
    }
} 