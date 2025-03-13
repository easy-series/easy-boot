package com.easy.id.config;

import com.easy.id.core.IdGenerator;
import com.easy.id.core.impl.SegmentIdGenerator;
import com.easy.id.core.impl.SnowflakeIdGenerator;
import com.easy.id.core.service.SegmentService;
import com.easy.id.core.service.impl.DbSegmentServiceImpl;
import com.easy.id.core.service.impl.RedisSegmentServiceImpl;
import com.easy.id.properties.IdGeneratorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ID生成器自动配置
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(IdGeneratorProperties.class)
public class IdGeneratorAutoConfiguration {

    /**
     * 创建基于雪花算法的ID生成器
     */
    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    @ConditionalOnProperty(name = "id-generator.type", havingValue = "snowflake", matchIfMissing = true)
    public SnowflakeIdGenerator snowflakeIdGenerator(IdGeneratorProperties properties) {
        log.info("初始化雪花算法ID生成器");
        return new SnowflakeIdGenerator(properties.getSnowflake().getWorkerId());
    }

    /**
     * 创建基于数据库的号段服务
     */
    @Bean
    @ConditionalOnMissingBean(SegmentService.class)
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnProperty(name = "id-generator.type", havingValue = "segment-db")
    public SegmentService dbSegmentService(JdbcTemplate jdbcTemplate, IdGeneratorProperties properties) {
        log.info("初始化基于数据库的号段服务");
        return new DbSegmentServiceImpl(jdbcTemplate, properties.getSegment().getDb().getTableName());
    }

    /**
     * 创建基于Redis的号段服务
     */
    @Bean
    @ConditionalOnMissingBean(SegmentService.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnProperty(name = "id-generator.type", havingValue = "segment-redis")
    public SegmentService redisSegmentService(StringRedisTemplate redisTemplate, IdGeneratorProperties properties) {
        log.info("初始化基于Redis的号段服务");
        return new RedisSegmentServiceImpl(redisTemplate, properties.getSegment().getRedis().getKeyPrefix());
    }

    /**
     * 创建基于号段的ID生成器
     */
    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    @ConditionalOnBean(SegmentService.class)
    public SegmentIdGenerator segmentIdGenerator(SegmentService segmentService, IdGeneratorProperties properties) {
        log.info("初始化号段模式ID生成器");
        return new SegmentIdGenerator(segmentService, properties.getSegment().getStep());
    }
}