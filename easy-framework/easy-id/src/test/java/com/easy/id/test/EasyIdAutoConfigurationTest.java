package com.easy.id.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.easy.id.config.IdProperties;
import com.easy.id.core.IdGenerator;
import com.easy.id.template.IdTemplate;

/**
 * Easy-ID 自动装配测试类
 * <p>
 * 测试Easy-ID模块的自动装配功能
 *
 * @author 芋道源码
 */
@SpringBootTest(classes = EasyIdAutoConfigurationTest.TestConfig.class)
@ActiveProfiles("test")
public class EasyIdAutoConfigurationTest {

    /**
     * 测试配置类，启用自动配置
     */
    @Configuration
    @EnableAutoConfiguration
    @EnableTransactionManagement
    public static class TestConfig {
        // 空配置类，用于引导Spring Boot应用上下文
    }

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private IdTemplate idTemplate;

    @Autowired(required = false)
    private IdProperties idProperties;

    /**
     * 测试是否正确自动装配了IdProperties配置类
     */
    @Test
    public void testIdPropertiesAutowired() {
        System.out.println("===== 测试IdProperties自动装配 =====");

        // 断言IdProperties被成功注入
        assertNotNull(idProperties, "IdProperties 应该被成功注入");

        // 验证配置属性是否正确加载
        System.out.println("默认ID生成类型: " + idProperties.getDefaultType());
        System.out.println("雪花算法配置: workerId=" + idProperties.getSnowflake().getWorkerId()
                + ", dataCenterId=" + idProperties.getSnowflake().getDataCenterId());
        System.out.println("号段模式配置: tableName=" + idProperties.getSegment().getTableName());

        // 检查业务配置
        if (idProperties.getSegment().getBizConfigs() != null && !idProperties.getSegment().getBizConfigs().isEmpty()) {
            System.out.println("号段模式业务配置:");
            idProperties.getSegment().getBizConfigs().forEach((key, config) -> System.out
                    .println("  " + key + ": bizKey=" + config.getBizKey() + ", step=" + config.getStep()));
        }

        // 注意：这里的断言应该与application-test.yml中的实际配置保持一致
        assertEquals(idProperties.getDefaultType(), "redis-segment", "默认ID类型应该是redis-segment");
    }

    /**
     * 测试是否正确自动装配了IdGenerator实现类
     */
    @Test
    public void testIdGeneratorBeans() {
        System.out.println("===== 测试IdGenerator Bean自动装配 =====");

        // 获取所有IdGenerator类型的Bean
        Map<String, IdGenerator> generators = context.getBeansOfType(IdGenerator.class);

        // 应该至少有一个生成器
        assertFalse(generators.isEmpty(), "应该至少有一个IdGenerator Bean");

        // 打印所有生成器
        System.out.println("已自动装配的ID生成器:");
        generators.forEach((name, generator) -> System.out
                .println(name + ": " + generator.getClass().getSimpleName() + " [" + generator.getName() + "]"));
    }

    /**
     * 测试是否正确自动装配了IdTemplate
     */
    @Test
    public void testIdTemplateAutowired() {
        System.out.println("===== 测试IdTemplate自动装配 =====");

        // 断言IdTemplate被成功注入
        assertNotNull(idTemplate, "IdTemplate 应该被成功注入");

        // 验证IdTemplate中的生成器是否正确配置
        Map<String, IdGenerator> generators = idTemplate.getGenerators();
        assertFalse(generators.isEmpty(), "IdTemplate中应该至少配置了一个生成器");

        // 打印生成器信息
        System.out.println("IdTemplate中的生成器:");
        generators.forEach((name, generator) -> System.out.println(name + ": " + generator.getClass().getSimpleName()));

        // 生成一些ID
        System.out.println("使用默认生成器生成的ID: " + idTemplate.nextId());
        System.out.println("批量生成3个ID:");
        long[] ids = idTemplate.nextId(3);
        for (int i = 0; i < ids.length; i++) {
            System.out.println("ID " + (i + 1) + ": " + ids[i]);
        }

        // 确认生成的ID的唯一性
        assertTrue(ids.length >= 2 && ids[0] != ids[1], "生成的ID应该是唯一的");
    }

    /**
     * 测试在实际项目中使用ID生成器
     */
    @Test
    public void testUsingIdGeneratorInProject() {

        assertNotNull(context.getBean(DataSource.class), "DataSource should be created");
        assertNotNull(context.getBean(PlatformTransactionManager.class), "TransactionManager should be created");

        // 打印所有bean名称，帮助诊断
        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("=== 已创建的Bean列表 ===");
        for (String beanName : beanNames) {
            System.out.println(beanName);
        }

        System.out.println("===== 测试在项目中使用ID生成器 =====");

        // 模拟业务服务类注入IdTemplate
        BusinessService businessService = new BusinessService(idTemplate);

        // 使用业务服务类生成ID
        System.out.println("业务服务生成用户ID: " + businessService.createUser());
        System.out.println("业务服务生成订单ID: " + businessService.createOrder());

        // 测试批量生成
        long[] orderIds = businessService.createBatchOrders(5);
        System.out.println("业务服务批量生成5个订单ID:");
        for (int i = 0; i < orderIds.length; i++) {
            System.out.println("订单ID " + (i + 1) + ": " + orderIds[i]);
        }

        // 批量测试默认生成器
        long[] orderIds2 = businessService.createBatchOrders2(5);
        System.out.println("segment生成器product业务服务批量生成5个订单ID:");
        for (int i = 0; i < orderIds2.length; i++) {
            System.out.println("订单ID " + (i + 1) + ": " + orderIds2[i]);
        }

        assertNotNull(orderIds, "应该能够批量生成订单ID");
        assertEquals(5, orderIds.length, "应该生成5个订单ID");
    }

    /**
     * 模拟业务服务类
     */
    static class BusinessService {
        private final IdTemplate idTemplate;

        public BusinessService(IdTemplate idTemplate) {
            this.idTemplate = idTemplate;
        }

        public long createUser() {
            // 实际业务中，生成用户ID可能会使用特定的生成器
            try {
                // snowflake，如果不存在则使用默认生成器
                return idTemplate.nextId("snowflake");
            } catch (Exception e) {
                return idTemplate.nextId();
            }
        }

        public long createOrder() {
            // 实际业务中，生成订单ID可能会使用特定的生成器
            try {
                // 尝试使用segment生成器，如果不存在则使用默认生成器
                return idTemplate.nextId("segment");
            } catch (Exception e) {
                return idTemplate.nextId();
            }
        }

        public long[] createBatchOrders(int count) {
            // 批量生成订单ID
            try {
                // 尝试使用segment生成器，如果不存在则使用默认生成器
                return idTemplate.nextId("redis-segment", count);
            } catch (Exception e) {
                return idTemplate.nextId(count);
            }
        }

        public long[] createBatchOrders2(int count) {
            // 批量生成订单ID
            try {
                // 尝试使用segment生成器，如果不存在则使用默认生成器
                return idTemplate.nextIdByBizKey("segment", "product_id", count);
            } catch (Exception e) {
                return idTemplate.nextId(count);
            }
        }
    }
}