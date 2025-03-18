package com.easy.mq.redis.core.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.easy.mq.redis.core.message.AbstractRedisMessage;

/**
 * Redis消息拦截器测试类
 */
class RedisMessageInterceptorTest {

    private TestRedisMessageInterceptor interceptor;
    private TestRedisMessage message;

    @BeforeEach
    void setUp() {
        interceptor = new TestRedisMessageInterceptor();
        message = new TestRedisMessage();
    }

    @Test
    void testSendMessageInterception() {
        // 验证初始状态
        assertEquals(0, interceptor.getSendBeforeCount());
        assertEquals(0, interceptor.getSendAfterCount());

        // 执行拦截方法
        interceptor.sendMessageBefore(message);
        assertEquals(1, interceptor.getSendBeforeCount());
        assertEquals(0, interceptor.getSendAfterCount());

        interceptor.sendMessageAfter(message);
        assertEquals(1, interceptor.getSendBeforeCount());
        assertEquals(1, interceptor.getSendAfterCount());
    }

    @Test
    void testConsumeMessageInterception() {
        // 验证初始状态
        assertEquals(0, interceptor.getConsumeBeforeCount());
        assertEquals(0, interceptor.getConsumeAfterCount());

        // 执行拦截方法
        interceptor.consumeMessageBefore(message);
        assertEquals(1, interceptor.getConsumeBeforeCount());
        assertEquals(0, interceptor.getConsumeAfterCount());

        interceptor.consumeMessageAfter(message);
        assertEquals(1, interceptor.getConsumeBeforeCount());
        assertEquals(1, interceptor.getConsumeAfterCount());
    }

    @Test
    void testMessageHeaderManipulation() {
        // 初始化消息头
        message.addHeader("original-key", "original-value");

        // 在拦截器中修改消息头
        interceptor.sendMessageBefore(message);

        // 验证消息头已被修改
        assertEquals("original-value", message.getHeader("original-key"));
        assertEquals("test-value", message.getHeader("test-key"));
    }

    /**
     * 测试用的消息类
     */
    static class TestRedisMessage extends AbstractRedisMessage {
        // 使用基类提供的功能即可
    }

    /**
     * 测试用的拦截器类
     */
    static class TestRedisMessageInterceptor implements RedisMessageInterceptor {

        private int sendBeforeCount = 0;
        private int sendAfterCount = 0;
        private int consumeBeforeCount = 0;
        private int consumeAfterCount = 0;

        @Override
        public void sendMessageBefore(AbstractRedisMessage message) {
            sendBeforeCount++;
            message.addHeader("test-key", "test-value");
        }

        @Override
        public void sendMessageAfter(AbstractRedisMessage message) {
            sendAfterCount++;
        }

        @Override
        public void consumeMessageBefore(AbstractRedisMessage message) {
            consumeBeforeCount++;
        }

        @Override
        public void consumeMessageAfter(AbstractRedisMessage message) {
            consumeAfterCount++;
        }

        public int getSendBeforeCount() {
            return sendBeforeCount;
        }

        public int getSendAfterCount() {
            return sendAfterCount;
        }

        public int getConsumeBeforeCount() {
            return consumeBeforeCount;
        }

        public int getConsumeAfterCount() {
            return consumeAfterCount;
        }
    }
}