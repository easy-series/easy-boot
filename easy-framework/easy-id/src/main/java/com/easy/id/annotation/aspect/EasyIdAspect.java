package com.easy.id.annotation.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.easy.id.annotation.EasyId;
import com.easy.id.core.IdGenerator;
import com.easy.id.template.IdTemplate;
import com.easy.id.template.IdTemplateGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * EasyId注解处理切面
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class EasyIdAspect {

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private IdTemplateGenerator templateGenerator;

    /**
     * 处理方法参数上的@EasyId注解
     */
    @Before("execution(* *(.., @com.easy.id.annotation.EasyId (*), ..))")
    public void processMethodParameterAnnotation(JoinPoint joinPoint) {
        log.debug("处理方法参数上的@EasyId注解");

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = methodSignature.getMethod().getParameters();
        Annotation[][] parameterAnnotations = methodSignature.getMethod().getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof EasyId) {
                    EasyId easyId = (EasyId) annotation;
                    Object arg = args[i];
                    if (arg != null) {
                        processEasyIdAnnotation(easyId, arg);
                    }
                }
            }
        }
    }

    /**
     * 处理对象字段上的@EasyId注解
     */
    @Before("execution(* *(@com.easy.id.annotation.EasyIdObject (*), ..))")
    public void processObjectAnnotation(JoinPoint joinPoint) {
        log.debug("处理对象字段上的@EasyId注解");

        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                processObjectFields(arg);
            }
        }
    }

    /**
     * 处理对象中字段上的@EasyId注解
     */
    private void processObjectFields(Object obj) {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            EasyId easyId = field.getAnnotation(EasyId.class);
            if (easyId != null) {
                field.setAccessible(true);
                try {
                    // 获取字段的值
                    Object fieldValue = field.get(obj);

                    // 如果字段值为null或为空字符串，则生成ID并设置
                    if (fieldValue == null ||
                            (fieldValue instanceof String && ((String) fieldValue).isEmpty())) {
                        String id = generateId(easyId);

                        // 设置ID值
                        if (field.getType() == String.class) {
                            field.set(obj, id);
                        } else if (field.getType() == Long.class || field.getType() == long.class) {
                            field.set(obj, Long.parseLong(id));
                        } else if (field.getType() == Integer.class || field.getType() == int.class) {
                            field.set(obj, Integer.parseInt(id));
                        }

                        log.debug("为字段 {} 生成ID: {}", field.getName(), id);
                    }
                } catch (Exception e) {
                    log.error("处理@EasyId注解失败", e);
                }
            }
        }
    }

    /**
     * 根据EasyId注解生成ID
     */
    private String generateId(EasyId easyId) {
        // 优先使用模板名称
        if (!StringUtils.isEmpty(easyId.template())) {
            return generateIdFromTemplate(easyId.template());
        }

        // 使用注解参数创建临时模板
        String businessKey = StringUtils.isEmpty(easyId.business()) ? "default" : easyId.business();

        // 如果是雪花算法
        if (easyId.useSnowflake()) {
            return String.valueOf(idGenerator.nextId(businessKey));
        }

        // 创建临时模板
        IdTemplate template = IdTemplate.builder()
                .name("temp_" + businessKey + "_" + System.nanoTime())
                .businessKey(businessKey)
                .dateFormat(easyId.dateFormat())
                .prefix(easyId.prefix())
                .separator(easyId.separator())
                .sequenceLength(easyId.sequenceLength())
                .useSnowflake(false)
                .build();

        // 注册临时模板
        templateGenerator.registerTemplate(template);

        // 生成ID
        return templateGenerator.nextId(template.getName());
    }

    /**
     * 从注册的模板生成ID
     */
    private String generateIdFromTemplate(String templateName) {
        // 检查模板是否存在
        if (!templateGenerator.templateExists(templateName)) {
            log.warn("模板不存在: {}, 使用雪花算法生成ID", templateName);
            return String.valueOf(idGenerator.nextId());
        }

        // 使用模板生成ID
        return templateGenerator.nextId(templateName);
    }

    /**
     * 处理EasyId注解
     */
    private void processEasyIdAnnotation(EasyId easyId, Object obj) {
        if (obj instanceof String) {
            // 如果参数是字符串类型且为空，则生成ID
            String strValue = (String) obj;
            if (strValue.isEmpty()) {
                String id = generateId(easyId);
                // 需要修改引用，但Java是值传递，所以这里无法直接修改参数值
                // 这里生成的ID无法赋值回参数，只能通过字段注入的方式处理
                log.warn("无法为方法参数生成ID，请使用字段注入方式");
            }
        } else {
            // 如果是复杂对象，处理其字段
            processObjectFields(obj);
        }
    }
}