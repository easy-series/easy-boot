package com.easy.id.test;

import com.easy.id.core.IdGenerator;
import com.easy.id.redis.RedisSegmentAllocator;
import com.easy.id.segment.SegmentIdGenerator;
import com.easy.id.segment.dao.DbSegmentAllocator;
import com.easy.id.segment.dao.SegmentAllocator;
import com.easy.id.snowflake.SnowflakeIdGenerator;
import com.easy.id.template.IdTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EasyId 在Spring Boot环境中的集成测试
 *
 * @author 芋道源码
 */
@SpringBootTest
@ActiveProfiles("test")
public class EasyIdSpringBootTest {

    @Autowired(required = false)
    private IdTemplate idTemplate;

    @Test
    public void testIdTemplateInjection() {
        // 如果未配置Redis等外部依赖，此处可能为null
        if (idTemplate != null) {
            assertNotNull(idTemplate, "IdTemplate 应该被成功注入");

            // 获取单个ID
            long id = idTemplate.nextId();
            assertTrue(id > 0, "生成的ID应该大于0");

            // 批量获取ID
            long[] ids = idTemplate.nextId(10);
            assertEquals(10, ids.length, "应该生成10个ID");

            // 检查ID唯一性
            Set<Long> idSet = new HashSet<>();
            for (long generatedId : ids) {
                assertTrue(generatedId > 0, "生成的ID应该大于0");
                idSet.add(generatedId);
            }
            assertEquals(10, idSet.size(), "生成的ID应该互不相同");

            // 获取所有注册的生成器
            Map<String, IdGenerator> generators = idTemplate.getGenerators();
            assertFalse(generators.isEmpty(), "应该至少有一个ID生成器被注册");

            // 打印所有生成器信息
            System.out.println("已注册的ID生成器:");
            for (Map.Entry<String, IdGenerator> entry : generators.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue().getClass().getSimpleName());
            }
        } else {
            System.out.println("IdTemplate未注入，跳过测试");
        }
    }

    @Test
    public void testAllIdGeneratorTypes() {
        if (idTemplate == null) {
            System.out.println("IdTemplate未注入，跳过测试");
            return;
        }

        Map<String, IdGenerator> generators = idTemplate.getGenerators();
        List<String> generatorNames = new ArrayList<>(generators.keySet());

        for (String name : generatorNames) {
            if ("default".equals(name)) {
                continue; // 跳过默认生成器，它是其他生成器的引用
            }

            System.out.println("\n测试 " + name + " 生成器:");

            // 生成几个ID
            for (int i = 0; i < 3; i++) {
                long id = idTemplate.nextId(name);
                System.out.println(name + " ID " + (i + 1) + ": " + id);
            }

            // 测试批量生成
            System.out.println("批量生成3个ID:");
            long[] ids = idTemplate.nextId(name, 3);
            for (int i = 0; i < ids.length; i++) {
                System.out.println(name + " 批量ID " + (i + 1) + ": " + ids[i]);
            }
        }
    }

    @Test
    public void testConcurrentIdGeneration() throws InterruptedException {
        // 如果未配置外部依赖，此处可能为null
        if (idTemplate == null) {
            System.out.println("IdTemplate未注入，跳过测试");
            return;
        }

        // 线程数和每个线程生成的ID数量
        int threadCount = 10;
        int idCountPerThread = 1000;

        // 用于存储所有生成的ID
        Set<Long> allIds = new HashSet<>(threadCount * idCountPerThread);

        // 线程同步器
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 启动多个线程同时生成ID
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 每个线程生成指定数量的ID
                    for (int j = 0; j < idCountPerThread; j++) {
                        long id = idTemplate.nextId();
                        synchronized (allIds) {
                            allIds.add(id);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await();
        executor.shutdown();

        // 验证生成的ID数量
        assertEquals(threadCount * idCountPerThread, allIds.size(),
                "并发生成的ID应该全部唯一，无重复");

        System.out.println("并发测试完成，成功生成 " + allIds.size() + " 个唯一ID");
    }

    /**
     * 测试配置类，用于创建测试所需的Bean
     */
    @SpringBootApplication
    static class TestConfig {

        /**
         * 创建雪花算法ID生成器
         */
        @Bean
        public IdGenerator snowflakeIdGenerator() {
            return new SnowflakeIdGenerator("snowflake", 1, 1);
        }

        /**
         * 事务管理器
         */
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        /**
         * 数据库号段分配器
         */
        @Bean
        public DbSegmentAllocator dbSegmentAllocator(DataSource dataSource,
                                                     PlatformTransactionManager transactionManager) {
            DbSegmentAllocator allocator = new DbSegmentAllocator(dataSource, transactionManager, "easy_id_allocator");
            // 初始化表结构和数据
            // allocator.init();
            return allocator;
        }

        /**
         * 数据库号段ID生成器
         */
        @Bean
        public IdGenerator dbSegmentIdGenerator(DbSegmentAllocator dbSegmentAllocator) {
            return new SegmentIdGenerator("segment", dbSegmentAllocator);
        }

        /**
         * Redis连接工厂
         */
        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName("localhost");
            config.setPort(6379);
            // 如有密码可以设置
            config.setPassword("123456");
            return new LettuceConnectionFactory(config);
        }

        /**
         * Redis模板
         */
        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();
            return template;
        }

        /**
         * Redis号段分配器
         */
        @Bean
        public SegmentAllocator redisSegmentAllocator(RedisTemplate<String, Object> redisTemplate) {
            return new RedisSegmentAllocator(redisTemplate, "easy:id:test:");
        }

        /**
         * Redis号段ID生成器
         */
        @Bean
        public IdGenerator redisSegmentIdGenerator(SegmentAllocator redisSegmentAllocator) {
            return new SegmentIdGenerator("redis-segment", redisSegmentAllocator);
        }

        /**
         * ID模板
         */
        @Bean
        @Primary
        public IdTemplate idTemplate(IdGenerator snowflakeIdGenerator,
                                     IdGenerator dbSegmentIdGenerator,
                                     IdGenerator redisSegmentIdGenerator) {
            // 创建ID模板
            IdTemplate template = new IdTemplate(snowflakeIdGenerator);

            // 注册其他生成器
            template.registerGenerator("segment", dbSegmentIdGenerator);
            template.registerGenerator("redis-segment", redisSegmentIdGenerator);

            return template;
        }
    }
}