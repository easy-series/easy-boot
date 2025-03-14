package com.easy.id.annotation;

import java.lang.annotation.*;

/**
 * 标记包含@EasyId注解字段的对象
 * 用于AOP切面识别需要处理的对象
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EasyIdObject {
    
} 