package com.easy.id.test;

import com.easy.id.core.IdGenerator;
import com.easy.id.snowflake.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ID生成器测试类
 *
 * @author 芋道源码
 */
public class IdGeneratorTest {

    /**
     * 测试雪花算法ID生成器
     */
    @Test
    public void testSnowflakeIdGenerator() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator("test-snowflake", 1, 1);

        // 测试生成ID的唯一性
        int count = 10000;
        Set<Long> idSet = new HashSet<>(count);

        for (int i = 0; i < count; i++) {
            long id = generator.nextId();
            assertTrue(id > 0, "ID应该大于0");
            assertFalse(idSet.contains(id), "ID不应该重复");
            idSet.add(id);
        }

        assertEquals(count, idSet.size(), "生成的ID数量应该等于请求数量");

        // 测试批量生成ID
        long[] ids = generator.nextId(100);
        assertEquals(100, ids.length, "批量生成的ID数量应该等于请求数量");

        // 验证批量生成的ID唯一性
        Set<Long> batchIdSet = new HashSet<>(100);
        for (long id : ids) {
            assertTrue(id > 0, "ID应该大于0");
            assertFalse(batchIdSet.contains(id), "批量生成的ID不应该重复");
            batchIdSet.add(id);
        }

        assertEquals(100, batchIdSet.size(), "批量生成的ID数量应该等于请求数量");

        // 验证生成的ID是否递增
        long lastId = 0;
        for (int i = 0; i < 10; i++) {
            long id = generator.nextId();
            assertTrue(id > lastId, "生成的ID应该递增");
            lastId = id;
        }
    }

    /**
     * 测试ID生成性能
     */
    @Test
    public void testPerformance() {
        IdGenerator generator = new SnowflakeIdGenerator("perf-test", 1, 1);

        // 预热
        for (int i = 0; i < 1000; i++) {
            generator.nextId();
        }

        // 测试单个ID生成性能
        int count = 100000;
        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            generator.nextId();
        }

        long end = System.currentTimeMillis();
        long cost = end - start;

        System.out.println("生成 " + count + " 个ID耗时: " + cost + " ms");
        System.out.println("平均每秒生成ID数: " + (count * 1000 / cost));

        // 测试批量生成性能
        start = System.currentTimeMillis();
        int batchSize = 1000;
        int batchCount = 100;

        for (int i = 0; i < batchCount; i++) {
            generator.nextId(batchSize);
        }

        end = System.currentTimeMillis();
        cost = end - start;

        System.out.println("批量生成 " + (batchSize * batchCount) + " 个ID耗时: " + cost + " ms");
        System.out.println("批量模式下平均每秒生成ID数: " + (batchSize * batchCount * 1000 / cost));
    }
}