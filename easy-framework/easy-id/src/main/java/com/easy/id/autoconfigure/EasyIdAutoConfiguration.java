package com.easy.id.autoconfigure;

import com.easy.id.config.IdProperties;
import com.easy.id.core.IdGenerator;
import com.easy.id.monitor.MonitoredIdGenerator;
import com.easy.id.redis.RedisSegmentAllocator;
import com.easy.id.segment.SegmentIdGenerator;
import com.easy.id.segment.dao.DbSegmentAllocator;
import com.easy.id.segment.dao.SegmentAllocator;
import com.easy.id.snowflake.SnowflakeIdGenerator;
import com.easy.id.template.IdTemplate;
import com.easy.redis.autoconfigure.YudaoRedisAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * ID生成器自动配置类
 *
 * @author 芋道源码
 */
@Slf4j
@AutoConfiguration(after = {YudaoRedisAutoConfiguration.class, DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class})
@EnableConfigurationProperties(IdProperties.class)
public class EasyIdAutoConfiguration {

    /**
     * 配置雪花算法ID生成器
     */
    @Bean
    @ConditionalOnProperty(prefix = "easy.id.snowflake", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "snowflakeIdGenerator")
    public IdGenerator snowflakeIdGenerator(IdProperties properties) {
        IdProperties.SnowflakeProperties snowflakeProps = properties.getSnowflake();
        log.info("初始化雪花算法ID生成器，数据中心ID: {}, 工作节点ID: {}", snowflakeProps.getDataCenterId(), snowflakeProps.getWorkerId());

        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(
                snowflakeProps.getName(),
                snowflakeProps.getWorkerId(),
                snowflakeProps.getDataCenterId());

        return new MonitoredIdGenerator(generator);
    }

    /**
     * 配置号段模式ID生成器
     */
    @Bean
    @ConditionalOnProperty(prefix = "easy.id.segment", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "segmentIdGenerator")
    @ConditionalOnClass({DataSource.class, PlatformTransactionManager.class})
    @ConditionalOnBean({DataSource.class, PlatformTransactionManager.class})
    public IdGenerator segmentIdGenerator(
            IdProperties properties,
            @Autowired(required = false) DataSource dataSource,
            @Autowired(required = false) PlatformTransactionManager transactionManager) {

        // 检查依赖是否存在
        if (dataSource == null || transactionManager == null) {
            log.warn("初始化号段模式ID生成器失败：DataSource或PlatformTransactionManager不存在");
            return null;
        }

        IdProperties.SegmentProperties segmentProps = properties.getSegment();
        log.info("初始化号段模式ID生成器，表名: {}", segmentProps.getTableName());

        // 创建数据库号段分配器
        DbSegmentAllocator allocator = new DbSegmentAllocator(dataSource, transactionManager,
                segmentProps.getTableName());

        // 创建号段模式ID生成器
        SegmentIdGenerator generator = new SegmentIdGenerator(segmentProps.getName(), allocator);

        return new MonitoredIdGenerator(generator);
    }

    /**
     * 配置Redis号段模式ID生成器
     */
    @Bean
    @ConditionalOnProperty(prefix = "easy.id.redis-segment", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "redisSegmentIdGenerator")
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnClass(RedisTemplate.class)
    public IdGenerator redisSegmentIdGenerator(
            IdProperties properties,
            RedisTemplate<String, Object> redisTemplate) {

        IdProperties.RedisSegmentProperties redisSegmentProps = properties.getRedisSegment();
        log.info("初始化Redis号段模式ID生成器，键前缀: {}", redisSegmentProps.getKeyPrefix());

        // 创建Redis号段分配器
        SegmentAllocator allocator = new RedisSegmentAllocator(redisTemplate, redisSegmentProps.getKeyPrefix());

        // 创建号段模式ID生成器
        SegmentIdGenerator generator = new SegmentIdGenerator(redisSegmentProps.getName(), allocator);

        return new MonitoredIdGenerator(generator);
    }

    /**
     * 配置ID模板
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(IdTemplate.class)
    public IdTemplate idTemplate(IdProperties properties, IdGenerator... generators) {
        IdTemplate template = new IdTemplate(null);

        // 注册所有生成器
        for (IdGenerator generator : generators) {
            template.registerGenerator(generator.getName(), generator);
        }

        // 设置默认生成器
        String defaultType = properties.getDefaultType();
        for (IdGenerator generator : generators) {
            if (generator.getName().equalsIgnoreCase(defaultType)) {
                template.setDefaultGenerator(generator);
                break;
            }
        }

        // 如果没有找到默认类型的生成器，使用第一个作为默认
        if (!template.getGenerators().isEmpty() && !template.getGenerators().containsKey("default")) {
            template.setDefaultGenerator(generators[0]);
        }

        return template;
    }

}