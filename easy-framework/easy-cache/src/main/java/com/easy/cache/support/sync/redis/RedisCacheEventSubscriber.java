package com.easy.cache.support.sync.redis;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.alibaba.fastjson2.JSON;
import com.easy.cache.support.sync.CacheEvent;
import com.easy.cache.support.sync.CacheEventSubscriber;
import com.easy.cache.support.sync.LocalCacheSyncManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于Redis的缓存事件订阅器实现
 */
@Slf4j
@RequiredArgsConstructor
public class RedisCacheEventSubscriber implements CacheEventSubscriber, MessageListener {

    /**
     * Redis消息监听容器
     */
    private final RedisMessageListenerContainer listenerContainer;

    /**
     * 本地缓存同步管理器
     */
    private final LocalCacheSyncManager syncManager;

    /**
     * 广播通道
     */
    private final String broadcastChannel;

    /**
     * 主题
     */
    private ChannelTopic topic;

    @Override
    public void subscribe() {
        topic = new ChannelTopic(broadcastChannel);
        listenerContainer.addMessageListener(this, topic);
        log.info("订阅缓存事件通道: {}", broadcastChannel);
    }

    @Override
    public void unsubscribe() {
        if (topic != null) {
            listenerContainer.removeMessageListener(this, topic);
            log.info("取消订阅缓存事件通道: {}", broadcastChannel);
            topic = null;
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            CacheEvent event = JSON.parseObject(body, CacheEvent.class);

            log.debug("接收到缓存事件: {}", event);

            // 处理缓存事件
            syncManager.handleCacheEvent(event);
        } catch (Exception e) {
            log.error("处理缓存事件失败", e);
        }
    }
}