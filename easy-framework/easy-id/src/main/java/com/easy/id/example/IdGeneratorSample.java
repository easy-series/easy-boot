package com.easy.id.example;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.easy.id.annotation.EasyId;
import com.easy.id.annotation.EasyIdObject;
import com.easy.id.core.IdGenerator;
import com.easy.id.template.IdTemplate;
import com.easy.id.template.IdTemplateGenerator;
import com.easy.id.utils.IdGeneratorUtil;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * ID生成器使用示例
 */
@Slf4j
@Component
public class IdGeneratorSample {

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private IdTemplateGenerator templateGenerator;

    /**
     * 演示基本用法
     */
    public void basicUsage() {
        // 1. 直接使用注入的IdGenerator
        long id1 = idGenerator.nextId();
        log.info("基本ID: {}", id1);

        // 2. 带业务标识
        long id2 = idGenerator.nextId("user");
        log.info("业务ID (user): {}", id2);

        // 3. 解析时间戳（仅雪花算法）
        long timestamp = idGenerator.parseTime(id1);
        if (timestamp > 0) {
            log.info("解析时间戳: {} -> {}", timestamp, new Date(timestamp));
        }

        // 4. 使用工具类
        long id3 = IdGeneratorUtil.nextId("order");
        log.info("工具类生成ID (order): {}", id3);
    }

    /**
     * 演示模板用法
     */
    public void templateUsage() {
        // 1. 注册日期前缀模板
        IdTemplate orderTemplate = IdTemplate.datePrefix("order", "yyyyMMdd", 6);
        templateGenerator.registerTemplate(orderTemplate);

        // 2. 使用模板生成ID
        String orderId = templateGenerator.nextId("order");
        log.info("订单ID: {}", orderId); // 例如: 202403140000001

        // 3. 创建自定义前缀模板
        IdTemplate userTemplate = IdTemplate.builder()
                .name("user")
                .businessKey("user")
                .prefix("U")
                .separator("-")
                .sequenceLength(8)
                .build();
        templateGenerator.registerTemplate(userTemplate);

        // 4. 使用自定义模板
        String userId = templateGenerator.nextId("user");
        log.info("用户ID: {}", userId); // 例如: U-00000001

        // 5. 使用雪花算法模板
        IdTemplate snowflakeTemplate = IdTemplate.snowflake("product");
        templateGenerator.registerTemplate(snowflakeTemplate);

        String productId = templateGenerator.nextId("product");
        log.info("产品ID: {}", productId); // 例如: 1234567890123456789

        // 6. 通过工具类使用模板
        String userId2 = IdGeneratorUtil.nextIdByTemplate("user");
        log.info("工具类生成用户ID: {}", userId2);
    }

    /**
     * 演示注解用法
     */
    public void annotationUsage() {
        // 创建用户对象
        User user = new User();
        // ID会在保存时自动生成
        saveUser(user);
        log.info("注解生成用户ID: {}", user.getId());

        // 创建订单对象
        Order order = new Order();
        // ID会在保存时自动生成（使用日期前缀）
        saveOrder(order);
        log.info("注解生成订单号: {}", order.getOrderNo());
    }

    /**
     * 模拟保存用户
     * 
     * @param user 用户对象 (使用@EasyIdObject注解标记包含@EasyId注解的对象)
     */
    public void saveUser(@EasyIdObject User user) {
        log.info("保存用户: {}", user);
        // 实际业务逻辑...
    }

    /**
     * 模拟保存订单
     * 
     * @param order 订单对象
     */
    public void saveOrder(@EasyIdObject Order order) {
        log.info("保存订单: {}", order);
        // 实际业务逻辑...
    }

    /**
     * 用户实体类
     */
    @Data
    public static class User {
        // 使用雪花算法生成ID
        @EasyId(useSnowflake = true)
        private Long id;

        private String name;
        private String email;
    }

    /**
     * 订单实体类
     */
    @Data
    public static class Order {
        private Long id;

        // 使用日期前缀的ID
        @EasyId(prefix = "ORD", dateFormat = "yyyyMMdd", sequenceLength = 6)
        private String orderNo;

        private double amount;
        private String status;
    }
}