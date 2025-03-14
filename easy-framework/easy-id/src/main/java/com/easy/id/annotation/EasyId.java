package com.easy.id.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 简易ID注解，用于标记需要自动生成ID的字段或方法参数
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EasyId {

    /**
     * 业务标识，用于区分不同业务线
     */
    String business() default "";

    /**
     * 模板名称，用于使用预定义的ID模板
     * 如果不指定，则使用business作为模板名称
     */
    String template() default "";

    /**
     * 是否使用雪花算法生成ID
     * 当template未指定且useSnowflake为true时，直接使用雪花算法生成ID
     */
    boolean useSnowflake() default false;

    /**
     * 日期格式，如yyyyMMdd等
     * 当template未指定时有效
     */
    String dateFormat() default "";

    /**
     * 前缀
     * 当template未指定时有效
     */
    String prefix() default "";

    /**
     * 分隔符
     * 当template未指定时有效
     */
    String separator() default "";

    /**
     * 序列号长度，用于左填充0
     * 当template未指定时有效
     */
    int sequenceLength() default 6;
}