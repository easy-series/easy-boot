package com.easy.cache.sync.redis;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.serialization.Serializer;
import com.easy.cache.sync.CacheEvent;
import com.easy.cache.sync.CacheEventSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Redis实现的缓存事件订阅器
 * 使用Redis的PubSub机制来订阅缓存事件
 */
public class RedisEventSubscriber implements CacheEventSubscriber, MessageListener {

    /**
     * Redis频道前缀
     */
    private static final String CHANNEL_PREFIX = "cache:event:";

    /**
     * Redis消息监听容器
     */
    private final RedisMessageListenerContainer listenerContainer;

    /**
     * 序列化器
     */
    private final Serializer serializer;

    /**
     * 缓存管理器
     */
    private final CacheManager cacheManager;

    /**
     * JSON工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造方法
     *
     * @param listenerContainer Redis消息监听容器
     * @param serializer        序列化器
     * @param cacheManager      缓存管理器
     */
    public RedisEventSubscriber(RedisMessageListenerContainer listenerContainer,
            Serializer serializer,
            CacheManager cacheManager) {
        this.listenerContainer = listenerContainer;
        this.serializer = serializer;
        this.cacheManager = cacheManager;
    }

    /**
     * 订阅指定缓存的事件
     *
     * @param cacheName 缓存名称
     */
    public void subscribe(String cacheName) {
        ChannelTopic topic = new ChannelTopic(buildChannelName(cacheName));
        listenerContainer.addMessageListener(this, topic);
    }

    /**
     * 取消订阅指定缓存的事件
     *
     * @param cacheName 缓存名称
     */
    public void unsubscribe(String cacheName) {
        ChannelTopic topic = new ChannelTopic(buildChannelName(cacheName));
        listenerContainer.removeMessageListener(this, topic);
    }

    /**
     * 处理Redis消息
     * 
     * @param message Redis消息
     * @param pattern 频道模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(pattern, StandardCharsets.UTF_8);
            // 从频道名称中提取缓存名称
            String cacheName = extractCacheName(channel);

            // 尝试多种方式解析消息
            CacheEvent event = deserializeMessage(message.getBody(), cacheName);

            // 如果事件为空，忽略该事件
            if (event == null) {
                System.err.println("无法解析缓存事件消息: " + new String(message.getBody(), StandardCharsets.UTF_8));
                return;
            }

            // 处理事件
            onMessage(event);
        } catch (Exception e) {
            System.err.println("处理Redis消息出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 尝试多种方式解析消息
     * 
     * @param body      消息体
     * @param cacheName 缓存名称
     * @return 缓存事件
     */
    private CacheEvent deserializeMessage(byte[] body, String cacheName) {
        // 尝试直接反序列化
        try {
            CacheEvent event = serializer.deserialize(body, CacheEvent.class);
            if (event != null && (event.getCacheName() == null || event.getCacheName().equals(cacheName))) {
                // 如果缓存名称为空，设置为从频道提取的名称
                if (event.getCacheName() == null) {
                    event.setCacheName(cacheName);
                }
                return event;
            }
        } catch (Exception e) {
            // 忽略异常，尝试其他反序列化方法
        }

        // 尝试将消息转为字符串后反序列化
        try {
            String jsonStr = new String(body, StandardCharsets.UTF_8);
            // 如果是被转义的JSON字符串，进行处理
            if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                jsonStr = jsonStr.substring(1, jsonStr.length() - 1).replace("\\\"", "\"");
            }
            CacheEvent event = objectMapper.readValue(jsonStr, CacheEvent.class);
            if (event != null && (event.getCacheName() == null || event.getCacheName().equals(cacheName))) {
                // 如果缓存名称为空，设置为从频道提取的名称
                if (event.getCacheName() == null) {
                    event.setCacheName(cacheName);
                }
                return event;
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }

        return null;
    }

    /**
     * 处理缓存事件
     * 
     * @param event 缓存事件
     */
    @Override
    public void onMessage(CacheEvent event) {
        if (cacheManager == null) {
            return;
        }

        switch (event.getEventType()) {
            case UPDATE:
                handleUpdate(event);
                break;
            case DELETE:
                handleDelete(event);
                break;
            case CLEAR:
                handleClear(event);
                break;
            default:
                // 未知事件类型，忽略
                break;
        }
    }

    /**
     * 处理更新事件
     * 
     * @param event 缓存事件
     */
    private void handleUpdate(CacheEvent event) {
        // 获取缓存实例
        Cache<Object, Object> cache = cacheManager.getCache(event.getCacheName());
        if (cache != null && event.getKey() != null) {
            // 更新缓存
            cache.put(event.getKey(), event.getValue());
        }
    }

    /**
     * 处理删除事件
     * 
     * @param event 缓存事件
     */
    private void handleDelete(CacheEvent event) {
        // 获取缓存实例
        Cache<Object, Object> cache = cacheManager.getCache(event.getCacheName());
        if (cache != null && event.getKey() != null) {
            // 删除缓存
            cache.remove(event.getKey());
        }
    }

    /**
     * 处理清空事件
     * 
     * @param event 缓存事件
     */
    private void handleClear(CacheEvent event) {
        // 获取缓存实例
        Cache<Object, Object> cache = cacheManager.getCache(event.getCacheName());
        if (cache != null) {
            // 清空缓存
            cache.clear();
        }
    }

    /**
     * 构建Redis频道名称
     * 
     * @param cacheName 缓存名称
     * @return Redis频道名称
     */
    private String buildChannelName(String cacheName) {
        return CHANNEL_PREFIX + cacheName;
    }

    /**
     * 从频道名称中提取缓存名称
     * 
     * @param channel 频道名称
     * @return 缓存名称
     */
    private String extractCacheName(String channel) {
        if (channel != null && channel.startsWith(CHANNEL_PREFIX)) {
            return channel.substring(CHANNEL_PREFIX.length());
        }
        return "";
    }
}