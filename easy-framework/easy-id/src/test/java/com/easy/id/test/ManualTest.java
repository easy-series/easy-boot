package com.easy.id.test;

import com.easy.id.core.IdGenerator;
import com.easy.id.monitor.MonitoredIdGenerator;
import com.easy.id.redis.RedisSegmentAllocator;
import com.easy.id.segment.SegmentIdGenerator;
import com.easy.id.segment.dao.DbSegmentAllocator;
import com.easy.id.segment.dao.SegmentRange;
import com.easy.id.snowflake.SnowflakeIdGenerator;
import com.easy.id.template.IdTemplate;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * 手动测试类，用于直接测试ID生成器
 * 这个类可以独立运行，不依赖Spring环境
 *
 * @author 芋道源码
 */
public class ManualTest {

    public static void main(String[] args) {
        System.out.println("====== 开始ID生成器测试 ======");

        // 测试雪花算法
        // testSnowflake();

        // 测试数据库号段模式
        // testDbSegment();

         // 测试Redis号段模式
         testRedisSegment();
        //
        // // 测试ID模板
        // testIdTemplate();
        //
        // // 测试ID生成性能
        // benchmarkPerformance();

        System.out.println("====== ID生成器测试完成 ======");
    }

    /**
     * 测试雪花算法ID生成器
     */
    private static void testSnowflake() {
        System.out.println("\n===== 测试雪花算法ID生成器 =====");

        // 创建雪花算法ID生成器
        IdGenerator generator = new SnowflakeIdGenerator("test-snowflake", 1, 1);

        // 添加监控
        generator = new MonitoredIdGenerator(generator);

        // 生成10个ID并打印
        System.out.println("生成10个ID:");
        for (int i = 0; i < 10; i++) {
            long id = generator.nextId();
            System.out.println("ID " + (i + 1) + ": " + id);
        }

        // 批量生成ID
        System.out.println("\n批量生成5个ID:");
        long[] ids = generator.nextId(5);
        for (int i = 0; i < ids.length; i++) {
            System.out.println("批量ID " + (i + 1) + ": " + ids[i]);
        }

        System.out.println("雪花算法ID生成器测试完成");
    }

    /**
     * 测试数据库号段模式
     */
    private static void testDbSegment() {
        System.out.println("\n===== 测试数据库号段模式 =====");

        DataSource dataSource = null;
        DbSegmentAllocator allocator = null;

        try {
            System.out.println("1. 创建数据源和事务管理器");
            // 创建MySQL数据源
            dataSource = createMysqlDataSource();
            PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);

            System.out.println("2. 初始化数据库表结构");
            // 初始化表结构
            initMysqlTable(dataSource);

            System.out.println("3. 创建号段分配器");
            // 创建号段分配器
            String tableName = "easy_id_allocator";
            allocator = new DbSegmentAllocator(dataSource, transactionManager, tableName);

            System.out.println("4. 初始化号段分配器");
            // 确保表结构已初始化
            allocator.init();

            System.out.println("5. 验证号段分配器功能");
            // 直接测试号段分配器获取号段
            System.out.println("测试号段分配器获取号段范围:");
            SegmentRange defaultRange = allocator.nextRange("default", 1000);
            System.out.println("获取到default号段范围: min=" + defaultRange.getMin() + ", max=" + defaultRange.getMax()
                    + ", step=" + defaultRange.getStep());

            SegmentRange userRange = allocator.nextRange("user_id", 1000);
            System.out.println("获取到user_id号段范围: min=" + userRange.getMin() + ", max=" + userRange.getMax() + ", step="
                    + userRange.getStep());

            System.out.println("6. 创建ID生成器");
            // 创建号段ID生成器
            SegmentIdGenerator generator = new SegmentIdGenerator("test-db-segment", allocator);
            IdGenerator monitoredGenerator = new MonitoredIdGenerator(generator);

            System.out.println("7. 测试ID生成 - 默认业务键");
            // 生成10个ID并打印
            System.out.println("使用默认业务键生成10个ID:");
            for (int i = 0; i < 10; i++) {
                long id = monitoredGenerator.nextId();
                System.out.println("ID " + (i + 1) + ": " + id);
            }

            System.out.println("8. 测试ID生成 - 自定义业务键");
            // 使用自定义业务键
            System.out.println("\n使用自定义业务键(user_id)生成5个ID:");
            for (int i = 0; i < 5; i++) {
                long id = generator.nextId("user_id");
                System.out.println("用户ID " + (i + 1) + ": " + id);
            }

            System.out.println("9. 测试批量ID生成");
            // 测试批量生成
            System.out.println("\n批量生成5个ID:");
            long[] ids = monitoredGenerator.nextId(5);
            for (int i = 0; i < ids.length; i++) {
                System.out.println("批量ID " + (i + 1) + ": " + ids[i]);
            }

            System.out.println("数据库号段模式测试完成");

        } catch (Exception e) {
            System.err.println("测试数据库号段模式失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (allocator != null) {
                try {
//                    ((SegmentIdGenerator) ((MonitoredIdGenerator) allocator).getDelegate()).shutdown();
                } catch (Exception e) {
                    // 忽略
                }
            }
        }
    }

    /**
     * 测试Redis号段模式
     */
    private static void testRedisSegment() {
        System.out.println("\n===== 测试Redis号段模式 =====");

        LettuceConnectionFactory connectionFactory = null;
        try {
            System.out.println("1. 创建Redis连接");
            // 创建Redis连接工厂
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName("localhost");
            config.setPort(6379);
            // 如有密码
            config.setPassword("123456");

            connectionFactory = new LettuceConnectionFactory(config);
            connectionFactory.afterPropertiesSet();

            System.out.println("2. 创建Redis模板");
            // 创建Redis模板
            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(connectionFactory);
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            redisTemplate.setHashKeySerializer(new StringRedisSerializer());
            redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            redisTemplate.afterPropertiesSet();

            System.out.println("3. 初始化Redis测试数据");
            // 初始化Redis中的测试数据
            initRedisTestData(redisTemplate);

            System.out.println("4. 创建Redis号段分配器");
            // 创建Redis号段分配器
            RedisSegmentAllocator allocator = new RedisSegmentAllocator(redisTemplate, "easy:id:test:");

            System.out.println("5. 验证号段分配器功能");
            // 直接测试号段分配器获取号段
            SegmentRange defaultRange = allocator.nextRange("default", 1000);
            System.out.println("获取到default号段范围: min=" + defaultRange.getMin() + ", max=" + defaultRange.getMax()
                    + ", step=" + defaultRange.getStep());

            SegmentRange orderRange = allocator.nextRange("order_id", 2000);
            System.out.println("获取到order_id号段范围: min=" + orderRange.getMin() + ", max=" + orderRange.getMax()
                    + ", step=" + orderRange.getStep());

            System.out.println("6. 创建ID生成器");
            // 创建号段ID生成器
            SegmentIdGenerator generator = new SegmentIdGenerator("test-redis-segment", allocator);
            IdGenerator monitoredGenerator = new MonitoredIdGenerator(generator);

            System.out.println("7. 测试ID生成 - 默认业务键");
            // 生成10个ID并打印
            System.out.println("使用默认业务键生成10个ID:");
            for (int i = 0; i < 10; i++) {
                long id = monitoredGenerator.nextId();
                System.out.println("ID " + (i + 1) + ": " + id);
            }

            System.out.println("8. 测试ID生成 - 自定义业务键");
            // 使用自定义业务键
            System.out.println("\n使用自定义业务键(order_id)生成5个ID:");
            for (int i = 0; i < 5; i++) {
                long id = generator.nextId("order_id");
                System.out.println("订单ID " + (i + 1) + ": " + id);
            }

            System.out.println("9. 测试批量ID生成");
            // 测试批量生成
            System.out.println("\n批量生成5个ID:");
            long[] ids = monitoredGenerator.nextId(5);
            for (int i = 0; i < ids.length; i++) {
                System.out.println("批量ID " + (i + 1) + ": " + ids[i]);
            }

            System.out.println("Redis号段模式测试完成");

        } catch (Exception e) {
            System.err.println("测试Redis号段模式失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭连接工厂
            if (connectionFactory != null) {
                connectionFactory.destroy();
            }
        }
    }

    /**
     * 初始化Redis测试数据
     */
    private static void initRedisTestData(RedisTemplate<String, Object> redisTemplate) {
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

        // 初始化支付ID业务键
        redisTemplate.opsForValue().set(keyPrefix + "payment_id:maxId", 5000L);
        redisTemplate.opsForValue().set(keyPrefix + "payment_id:step", 1000);

        System.out.println("已初始化Redis测试数据，包括default、user_id、order_id、product_id、payment_id");
    }

    /**
     * 测试ID模板
     */
    private static void testIdTemplate() {
        System.out.println("\n===== 测试ID模板 =====");

        // 创建雪花算法ID生成器
        IdGenerator snowflakeGenerator = new SnowflakeIdGenerator("snowflake", 1, 1);

        // 创建ID模板
        IdTemplate template = new IdTemplate(snowflakeGenerator);

        // 使用默认生成器获取ID
        System.out.println("使用默认生成器获取ID: " + template.nextId());

        // 使用指定生成器获取ID
        System.out.println("使用snowflake生成器获取ID: " + template.nextId("snowflake"));

        // 批量获取ID
        System.out.println("批量获取3个ID:");
        long[] batchIds = template.nextId(3);
        for (int i = 0; i < batchIds.length; i++) {
            System.out.println("批量ID " + (i + 1) + ": " + batchIds[i]);
        }

        System.out.println("ID模板测试完成");
    }

    /**
     * 性能测试
     */
    private static void benchmarkPerformance() {
        System.out.println("\n===== 性能测试 =====");

        // 创建雪花算法ID生成器
        IdGenerator generator = new SnowflakeIdGenerator("perf-test", 1, 1);

        // 预热
        for (int i = 0; i < 1000; i++) {
            generator.nextId();
        }

        // 测试单个ID生成性能
        int count = 100000;
        Set<Long> ids = new HashSet<>(count);

        System.out.println("测试生成 " + count + " 个ID的性能...");
        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            ids.add(generator.nextId());
        }

        long end = System.currentTimeMillis();
        long cost = end - start;

        System.out.println("生成 " + count + " 个ID耗时: " + cost + " ms");
        System.out.println("平均每秒生成ID数: " + (count * 1000L / cost));
        System.out.println("唯一ID数量: " + ids.size() + " (应该等于 " + count + ")");

        // 测试批量生成性能
        System.out.println("\n测试批量生成ID的性能...");
        int batchSize = 1000;
        int batchCount = 100;

        start = System.currentTimeMillis();

        for (int i = 0; i < batchCount; i++) {
            generator.nextId(batchSize);
        }

        end = System.currentTimeMillis();
        cost = end - start;

        System.out.println("批量生成 " + (batchSize * batchCount) + " 个ID耗时: " + cost + " ms");
        System.out.println("批量模式下平均每秒生成ID数: " + (batchSize * batchCount * 1000L / cost));

        System.out.println("性能测试完成");
    }

    /**
     * 创建MySQL数据源
     */
    private static DataSource createMysqlDataSource() {
        System.out.println("创建MySQL数据源 - 连接到localhost:3306/easy_boot");
        MysqlDataSource ds = new MysqlDataSource();
        ds.setURL(
                "jdbc:mysql://localhost:3306/easy_boot?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai");
        ds.setUser("root");
        ds.setPassword("123456");

        // 测试连接
        try (Connection conn = ds.getConnection()) {
            System.out.println("数据库连接成功: " + conn.getMetaData().getDatabaseProductName() + " "
                    + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println("数据库连接失败: " + e.getMessage());
            throw new RuntimeException("数据库连接失败", e);
        }

        return ds;
    }

    /**
     * 初始化MySQL表
     */
    private static void initMysqlTable(DataSource dataSource) throws Exception {
        System.out.println("初始化MySQL表结构和数据");

        try (Connection conn = dataSource.getConnection()) {
            System.out.println("获取数据库连接成功");

            // 创建表
            System.out.println("创建ID分配表(如果不存在)...");
            try (Statement stmt = conn.createStatement()) {
                // 创建ID分配表
                String createTableSql = "CREATE TABLE IF NOT EXISTS `easy_id_allocator` (\n" +
                        "  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',\n" +
                        "  `biz_key` VARCHAR(128) NOT NULL COMMENT '业务键',\n" +
                        "  `max_id` BIGINT NOT NULL COMMENT '当前最大ID',\n" +
                        "  `step` INT NOT NULL COMMENT '步长',\n" +
                        "  `version` INT NOT NULL COMMENT '版本号',\n" +
                        "  `description` VARCHAR(256) DEFAULT NULL COMMENT '描述',\n" +
                        "  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',\n"
                        +
                        "  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n" +
                        "  PRIMARY KEY (`id`),\n" +
                        "  UNIQUE KEY `uk_biz_key` (`biz_key`)\n" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID分配表';";
                stmt.execute(createTableSql);
                System.out.println("表创建成功或已存在");
            }

            // 检查表是否为空
            System.out.println("检查表中是否有数据...");
            int count = 0;
            try (Statement stmt = conn.createStatement()) {
                String countSql = "SELECT COUNT(*) FROM `easy_id_allocator`";
                ResultSet rs = stmt.executeQuery(countSql);
                rs.next();
                count = rs.getInt(1);
                System.out.println("现有记录数: " + count);
            }

            // 清空表并插入初始数据 (为了测试，总是重新插入数据)
            System.out.println("清空表并插入测试数据...");
            try (Statement stmt = conn.createStatement()) {
                // 清空表
                stmt.execute("TRUNCATE TABLE `easy_id_allocator`");

                // 插入初始数据
                String insertSql = "INSERT INTO `easy_id_allocator` (`biz_key`, `max_id`, `step`, `version`, `description`) VALUES "
                        +
                        "('default', 0, 1000, 1, '默认业务'), " +
                        "('user_id', 10000, 1000, 1, '用户ID'), " +
                        "('order_id', 100000, 2000, 1, '订单ID'), " +
                        "('product_id', 1000, 500, 1, '商品ID'), " +
                        "('payment_id', 5000, 1000, 1, '支付ID');";
                stmt.execute(insertSql);
                System.out.println("已插入初始数据");
            }

            // 验证数据
            System.out.println("验证数据插入成功...");
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt
                        .executeQuery("SELECT biz_key, max_id, step FROM `easy_id_allocator` ORDER BY biz_key");
                while (rs.next()) {
                    System.out.println(String.format("业务键: %-10s 最大ID: %-8d 步长: %d",
                            rs.getString("biz_key"), rs.getLong("max_id"), rs.getInt("step")));
                }
            }
        }

        System.out.println("数据库初始化完成");
    }
}