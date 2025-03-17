package com.easy.id.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.easy.id.exception.IdGeneratorException;
import com.easy.id.redis.RedisSegmentAllocator;
import com.easy.id.segment.SegmentIdGenerator;
import com.easy.id.snowflake.SnowflakeIdGenerator;
import com.easy.id.template.IdTemplate;

/**
 * IdTemplate测试类
 * 
 * 测试IdTemplate的基本功能和业务键功能
 */
public class IdTemplateTest {

    private IdTemplate idTemplate;
    private SnowflakeIdGenerator snowflakeGenerator;
    private SegmentIdGenerator segmentGenerator;
    private RedisTemplate<String, Object> redisTemplate;
    private RedissonConnectionFactory connectionFactory;
    private RedissonClient redissonClient;

    @BeforeEach
    public void setup() {
        // 创建雪花算法生成器
        snowflakeGenerator = new SnowflakeIdGenerator("snowflake", 1, 1);

        // 创建Redisson配置
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setPassword("123456")
                .setDatabase(0);

        // 创建Redisson客户端
        redissonClient = Redisson.create(config);

        // 创建Redis连接工厂
        connectionFactory = new RedissonConnectionFactory(redissonClient);

        // 创建Redis模板
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();

        // 初始化Redis测试数据
        initRedisTestData(redisTemplate);

        // 创建Redis号段分配器
        RedisSegmentAllocator allocator = new RedisSegmentAllocator(redisTemplate, "easy:id:test:");

        // 创建号段ID生成器
        segmentGenerator = new SegmentIdGenerator("segment", allocator);

        // 创建ID模板
        idTemplate = new IdTemplate(snowflakeGenerator);
        idTemplate.registerGenerator("segment", segmentGenerator);
    }

    /**
     * 初始化Redis测试数据
     */
    private void initRedisTestData(RedisTemplate<String, Object> redisTemplate) {
        String keyPrefix = "easy:id:test:";

        // 初始化默认业务键
        redisTemplate.opsForValue().set(keyPrefix + "default:maxId", 0L);
        redisTemplate.opsForValue().set(keyPrefix + "default:step", 1000);

        // 初始化用户ID业务键
        redisTemplate.opsForValue().set(keyPrefix + "user_id:maxId", 10000L);
        redisTemplate.opsForValue().set(keyPrefix + "user_id:step", 1000);

        // 初始化订单ID业务键
        redisTemplate.opsForValue().set(keyPrefix + "order_id:maxId", 100000L);
        redisTemplate.opsForValue().set(keyPrefix + "order_id:step", 2000);

        // 初始化商品ID业务键
        redisTemplate.opsForValue().set(keyPrefix + "product_id:maxId", 1000L);
        redisTemplate.opsForValue().set(keyPrefix + "product_id:step", 500);
    }

    @Test
    public void testBasicFunctionality() {
        // 测试使用默认生成器获取ID
        long id1 = idTemplate.nextId();
        assertNotNull(id1);

        // 测试使用指定生成器获取ID
        long id2 = idTemplate.nextId("snowflake");
        assertNotNull(id2);

        // 测试批量获取ID
        long[] ids = idTemplate.nextId(3);
        assertEquals(3, ids.length);
        assertTrue(ids[0] != ids[1] && ids[1] != ids[2], "生成的ID应该是唯一的");
    }

    @Test
    public void testBizKeyFunctionality() {
        // 测试使用默认生成器和业务键获取ID
        assertThrows(IdGeneratorException.class, () -> {
            idTemplate.nextIdByBizKey("user_id");
        }, "雪花算法生成器不支持业务键，应该抛出异常");

        // 测试使用号段生成器和业务键获取ID
        long id1 = idTemplate.nextIdByBizKey("segment", "user_id");
        assertTrue(id1 >= 10001 && id1 <= 11000, "ID应该在用户ID号段范围内");

        // 测试使用号段生成器和订单业务键获取ID
        long id2 = idTemplate.nextIdByBizKey("segment", "order_id");
        assertTrue(id2 >= 100001 && id2 <= 102000, "ID应该在订单ID号段范围内");

        // 测试批量获取ID
        long[] ids = idTemplate.nextIdByBizKey("segment", "product_id", 3);
        assertEquals(3, ids.length);
        assertTrue(ids[0] >= 1001 && ids[0] <= 1500, "ID应该在商品ID号段范围内");
    }

    @Test
    public void testMultipleGenerators() {
        // 设置号段生成器为默认生成器
        idTemplate.setDefaultGenerator(segmentGenerator);

        // 测试使用默认生成器获取ID
        long id1 = idTemplate.nextId();
        assertTrue(id1 >= 1 && id1 <= 1000, "ID应该在默认号段范围内");

        // 测试使用默认生成器和业务键获取ID
        long id2 = idTemplate.nextIdByBizKey("user_id");
        assertTrue(id2 >= 10001 && id2 <= 11000, "ID应该在用户ID号段范围内");

        // 测试使用雪花算法生成器获取ID
        long id3 = idTemplate.nextId("snowflake");
        assertNotNull(id3);
    }
}