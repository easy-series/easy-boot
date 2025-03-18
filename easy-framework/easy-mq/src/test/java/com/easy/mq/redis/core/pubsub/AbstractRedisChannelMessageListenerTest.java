package com.easy.mq.redis.core.pubsub;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;

import com.easy.mq.redis.core.RedisMQTemplate;
import com.easy.mq.redis.core.interceptor.RedisMessageInterceptor;

/**
 * Redis频道消息监听器测试类
 */
class AbstractRedisChannelMessageListenerTest {

    private TestRedisChannelMessageListener listener;

    @Mock
    private RedisMQTemplate redisMQTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisMessageInterceptor interceptor;

    @Mock
    private Message message;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 初始化测试监听器
        listener = new TestRedisChannelMessageListener();

        // 配置模拟对象
        List<RedisMessageInterceptor> interceptors = new ArrayList<>();
        interceptors.add(interceptor);

        when(redisMQTemplate.getRedisTemplate()).thenReturn(any());
        when(redisMQTemplate.getInterceptors()).thenReturn(interceptors);

        // 设置消息内容
        String jsonContent = "{\"content\":\"test-message\"}";
        when(message.getBody()).thenReturn(jsonContent.getBytes(StandardCharsets.UTF_8));

        // 设置RedisMQTemplate
        listener.setRedisMQTemplate(redisMQTemplate);
    }

    @Test
    void testOnMessage() {
        // 调用监听器的消息处理方法
        listener.onMessage(message, new byte[0]);

        // 验证拦截器方法被调用
        verify(interceptor).consumeMessageBefore(any(TestChannelMessage.class));
        verify(interceptor).consumeMessageAfter(any(TestChannelMessage.class));

        // 验证消息处理方法被调用
        assertTrue(listener.isMessageProcessed());
    }

    /**
     * 测试用的消息类
     */
    static class TestChannelMessage extends AbstractRedisChannelMessage {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String getChannel() {
            return "test-channel";
        }
    }

    /**
     * 测试用的监听器类
     */
    static class TestRedisChannelMessageListener extends AbstractRedisChannelMessageListener<TestChannelMessage> {

        private boolean messageProcessed = false;

        @Override
        public void onMessage(TestChannelMessage message) {
            // 标记消息已处理
            messageProcessed = true;
        }

        public boolean isMessageProcessed() {
            return messageProcessed;
        }
    }
}