package com.easy.id.template;

import lombok.Builder;
import lombok.Data;

/**
 * ID模板定义类
 * 用于定义ID的生成格式和规则
 */
@Data
@Builder
public class IdTemplate {

    /**
     * 模板名称
     */
    private String name;

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 包含时间格式，如: yyyyMMdd, yyyyMM等
     */
    private String dateFormat;

    /**
     * 分隔符，默认为空
     */
    private String separator;

    /**
     * 序列号长度，用于左填充0
     */
    private int sequenceLength;

    /**
     * 最大序列号
     */
    private long maxSequence;

    /**
     * 是否使用雪花算法的ID，默认为false
     */
    private boolean useSnowflake;

    /**
     * 业务标识，用于获取号段或区分业务
     */
    private String businessKey;

    /**
     * 创建一个简单的模板
     */
    public static IdTemplate simple(String businessKey) {
        return IdTemplate.builder()
                .name(businessKey)
                .businessKey(businessKey)
                .sequenceLength(6)
                .useSnowflake(false)
                .build();
    }

    /**
     * 创建一个日期前缀的模板
     */
    public static IdTemplate datePrefix(String businessKey, String dateFormat, int sequenceLength) {
        return IdTemplate.builder()
                .name(businessKey)
                .businessKey(businessKey)
                .dateFormat(dateFormat)
                .sequenceLength(sequenceLength)
                .useSnowflake(false)
                .build();
    }

    /**
     * 创建一个使用雪花算法的模板
     */
    public static IdTemplate snowflake(String businessKey) {
        return IdTemplate.builder()
                .name(businessKey)
                .businessKey(businessKey)
                .useSnowflake(true)
                .build();
    }

    /**
     * 创建一个自定义前缀的模板
     */
    public static IdTemplate withPrefix(String businessKey, String prefix, String separator) {
        return IdTemplate.builder()
                .name(businessKey)
                .businessKey(businessKey)
                .prefix(prefix)
                .separator(separator)
                .sequenceLength(6)
                .useSnowflake(false)
                .build();
    }

    /**
     * 创建一个完整格式的模板
     */
    public static IdTemplate full(String name, String businessKey, String prefix, String dateFormat,
            String separator, int sequenceLength, boolean useSnowflake) {
        return IdTemplate.builder()
                .name(name)
                .businessKey(businessKey)
                .prefix(prefix)
                .dateFormat(dateFormat)
                .separator(separator)
                .sequenceLength(sequenceLength)
                .useSnowflake(useSnowflake)
                .build();
    }
}