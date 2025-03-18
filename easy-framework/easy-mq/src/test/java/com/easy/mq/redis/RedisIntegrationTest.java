package com.easy.mq.redis;

import com.easy.mq.redis.config.RedisTestConfiguration;
import com.easy.mq.redis.core.RedisMQTemplate;
import com.easy.mq.redis.core.pubsub.AbstractRedisChannelMessage;
import com.easy.mq.redis.core.pubsub.AbstractRedisChannelMessageListener;
import com.easy.mq.redis.core.stream.AbstractRedisStreamMessage;
import com.easy.mq.redis.core.stream.AbstractRedisStreamMessageListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = RedisIntegrationTest.TestConfig.class)
class RedisIntegrationTest {

    @Autowired
    private RedisMQTemplate redisMQTemplate;

    @SpyBean
    private TestChannelMessageListener channelMessageListener;

    @SpyBean
    private TestStreamMessageListener streamMessageListener;

    @Test
    void testChannelMessageSendAndReceive() throws Exception {
        // 准备测试
        CountDownLatch latch = new CountDownLatch(1);
        channelMessageListener.setLatch(latch);

        // 发送消息
        TestChannelMessage message = new TestChannelMessage("这是一条测试Channel消息");
        redisMQTemplate.send(message);

        // 等待接收消息
        boolean received = latch.await(5, TimeUnit.SECONDS);

        // 验证结果
        assertTrue(received, "消息应该被接收到");
        verify(channelMessageListener).onMessage(any(TestChannelMessage.class));
        assertEquals("这是一条测试Channel消息", channelMessageListener.getLastMessage().getContent());
    }

    @Test
    void testStreamMessageSendAndReceive() {
        // 发送Stream消息
        TestStreamMessage message = new TestStreamMessage("这是一条测试Stream消息");
        RecordId recordId = redisMQTemplate.send(message);

        // 验证记录ID不为空
        assertTrue(recordId != null, "应该返回有效的Stream消息ID");
    }

    /**
     * 测试配置类
     */
    @Configuration
    @Import(RedisTestConfiguration.class)
    static class TestConfig {

        @Bean
        public TestChannelMessageListener testChannelMessageListener() {
            return new TestChannelMessageListener();
        }

        @Bean
        public TestStreamMessageListener testStreamMessageListener() {
            return new TestStreamMessageListener();
        }

        @Bean
        public RedisMessageListenerContainer redisMessageListenerContainer(
                RedisMQTemplate redisMQTemplate, TestChannelMessageListener listener) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(redisMQTemplate.getRedisTemplate().getRequiredConnectionFactory());
            listener.setRedisMQTemplate(redisMQTemplate);
            return container;
        }
    }

    /**
     * 测试用Channel消息
     */
    static class TestChannelMessage extends AbstractRedisChannelMessage {

        private String content;

        public TestChannelMessage() {
        }

        public TestChannelMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String getChannel() {
            return "test:channel";
        }
    }

    /**
     * 测试用Channel消息监听器
     */
    static class TestChannelMessageListener extends AbstractRedisChannelMessageListener<TestChannelMessage> {

        private TestChannelMessage lastMessage;
        private CountDownLatch latch;

        @Override
        public void onMessage(TestChannelMessage message) {
            this.lastMessage = message;
            if (latch != null) {
                latch.countDown();
            }
        }

        public TestChannelMessage getLastMessage() {
            return lastMessage;
        }

        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }
    }

    /**
     * 测试用Stream消息
     */
    static class TestStreamMessage extends AbstractRedisStreamMessage {

        private String content;

        public TestStreamMessage() {
        }

        public TestStreamMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String getStreamKey() {
            return "test:stream";
        }
    }

    /**
     * 测试用Stream消息监听器
     */
    static class TestStreamMessageListener extends AbstractRedisStreamMessageListener<TestStreamMessage> {

        private TestStreamMessage lastMessage;

        @Override
        public void onMessage(TestStreamMessage message) {
            this.lastMessage = message;
        }

        @Override
        public String getStreamKey() {
            return "test:stream";
        }

        @Override
        public String getGroup() {
            return "test-group";
        }

        public TestStreamMessage getLastMessage() {
            return lastMessage;
        }
    }
} 