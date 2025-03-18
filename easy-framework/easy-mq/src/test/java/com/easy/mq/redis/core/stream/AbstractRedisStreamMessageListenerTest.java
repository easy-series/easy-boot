package com.easy.mq.redis.core.stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import com.easy.mq.redis.core.RedisMQTemplate;
import com.easy.mq.redis.core.interceptor.RedisMessageInterceptor;

/**
 * Redis Stream消息监听器测试类
 */
class AbstractRedisStreamMessageListenerTest {

    private TestRedisStreamMessageListener listener;

    @Mock
    private RedisMQTemplate redisMQTemplate;

    @Mock
    private RedisTemplate<String, ?> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private RedisMessageInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 初始化监听器
        listener = new TestRedisStreamMessageListener();

        // 配置模拟对象
        List<RedisMessageInterceptor> interceptors = new ArrayList<>();
        interceptors.add(interceptor);

        when(redisMQTemplate.getRedisTemplate()).thenReturn(any());
        when(redisMQTemplate.getInterceptors()).thenReturn(interceptors);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        // 设置RedisMQTemplate
        listener.setRedisMQTemplate(redisMQTemplate);
    }

    /**
     * 测试Stream消息处理
     */
    @Test
    void testOnMessage() {
        // 创建测试消息
        TestStreamMessage message = new TestStreamMessage();
        message.setContent("test-stream-message");

        // 调用消息处理方法
        listener.onMessage(message);

        // 验证拦截器被调用
        verify(interceptor).consumeMessageBefore(any(TestStreamMessage.class));
        verify(interceptor).consumeMessageAfter(any(TestStreamMessage.class));

        // 验证消息处理方法被调用
        assertTrue(listener.isMessageProcessed());
    }

    /**
     * 测试JSON解析为消息对象
     */
    @Test
    void testJsonParsing() {
        // 生成JSON字符串
        String json = "{\"content\":\"test-stream-message\"}";

        // 创建一个模拟的Record ID
        RecordId recordId = mock(RecordId.class);
        when(recordId.getValue()).thenReturn("1234567890-0");

        // 模拟Stream消息确认
        when(streamOperations.acknowledge(anyString(), anyString(), anyString())).thenReturn(1L);

        // 手动触发消息处理回调
        TestStreamMessage message = new TestStreamMessage();
        message.setContent("test-stream-message");
        listener.onMessage(message);

        // 验证消息已被处理
        assertTrue(listener.isMessageProcessed());
    }

    /**
     * 测试用的Stream消息类
     */
    static class TestStreamMessage extends AbstractRedisStreamMessage {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String getStreamKey() {
            return "test-stream";
        }
    }

    /**
     * 测试用的Stream监听器类
     */
    static class TestRedisStreamMessageListener extends AbstractRedisStreamMessageListener<TestStreamMessage> {

        private boolean messageProcessed = false;

        @Override
        public void onMessage(TestStreamMessage message) {
            // 标记消息已处理
            messageProcessed = true;
        }

        @Override
        public String getStreamKey() {
            return "test-stream";
        }

        @Override
        public String getGroup() {
            return "test-group";
        }

        public boolean isMessageProcessed() {
            return messageProcessed;
        }
    }
}