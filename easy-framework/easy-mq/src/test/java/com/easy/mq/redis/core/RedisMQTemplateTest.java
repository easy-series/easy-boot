package com.easy.mq.redis.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import com.easy.mq.redis.core.interceptor.RedisMessageInterceptor;
import com.easy.mq.redis.core.pubsub.AbstractRedisChannelMessage;
import com.easy.mq.redis.core.stream.AbstractRedisStreamMessage;

class RedisMQTemplateTest {

    private RedisMQTemplate redisMQTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private RedisMessageInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        redisMQTemplate = new RedisMQTemplate(redisTemplate);
        redisMQTemplate.addInterceptor(interceptor);
    }

    @Test
    void testSendChannelMessage() {
        // 创建测试消息
        TestChannelMessage message = new TestChannelMessage("test-content");

        // 执行发送
        redisMQTemplate.send(message);

        // 验证消息拦截器被调用
        verify(interceptor).sendMessageBefore(message);
        verify(interceptor).sendMessageAfter(message);

        // 验证Redis模板的convertAndSend方法被调用
        verify(redisTemplate).convertAndSend(eq("test-channel"), any(String.class));
    }

    @Test
    void testSendStreamMessage() {
        // 创建测试消息
        TestStreamMessage message = new TestStreamMessage("test-stream-content");

        // 模拟Stream操作返回
        when(streamOperations.add(any())).thenReturn(mock(RecordId.class));

        // 执行发送
        redisMQTemplate.send(message);

        // 验证消息拦截器被调用
        verify(interceptor).sendMessageBefore(message);
        verify(interceptor).sendMessageAfter(message);

        // 验证Redis Stream操作被调用
        verify(streamOperations).add(any());
    }

    // 测试用的Channel消息类
    static class TestChannelMessage extends AbstractRedisChannelMessage {

        private String content;

        public TestChannelMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String getChannel() {
            return "test-channel";
        }
    }

    // 测试用的Stream消息类
    static class TestStreamMessage extends AbstractRedisStreamMessage {

        private String content;

        public TestStreamMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String getStreamKey() {
            return "test-stream";
        }
    }
}