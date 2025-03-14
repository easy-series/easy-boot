package com.easy.id.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.easy.id.core.IdGenerator;
import com.easy.id.exception.IdGeneratorException;
import com.easy.id.template.IdTemplate;
import com.easy.id.template.IdTemplateGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * ID生成器工具类
 */
@Slf4j
@Component
public class IdGeneratorUtil {

    private static IdGenerator idGenerator;
    private static IdTemplateGenerator templateGenerator;

    @Autowired
    public void setIdGenerator(IdGenerator idGenerator) {
        IdGeneratorUtil.idGenerator = idGenerator;
    }

    @Autowired
    public void setTemplateGenerator(IdTemplateGenerator templateGenerator) {
        IdGeneratorUtil.templateGenerator = templateGenerator;
    }

    /**
     * 获取下一个ID
     *
     * @return ID
     */
    public static long nextId() {
        if (idGenerator == null) {
            throw new IdGeneratorException("IdGenerator未初始化");
        }
        return idGenerator.nextId();
    }

    /**
     * 获取下一个ID（带业务标识）
     *
     * @param businessKey 业务标识
     * @return ID
     */
    public static long nextId(String businessKey) {
        if (idGenerator == null) {
            throw new IdGeneratorException("IdGenerator未初始化");
        }
        return idGenerator.nextId(businessKey);
    }

    /**
     * 解析ID中的时间戳
     *
     * @param id ID
     * @return 时间戳（毫秒）
     */
    public static long parseTime(long id) {
        if (idGenerator == null) {
            throw new IdGeneratorException("IdGenerator未初始化");
        }
        return idGenerator.parseTime(id);
    }

    /**
     * 解析ID中的序列号
     *
     * @param id ID
     * @return 序列号
     */
    public static long parseSequence(long id) {
        if (idGenerator == null) {
            throw new IdGeneratorException("IdGenerator未初始化");
        }
        return idGenerator.parseSequence(id);
    }

    /**
     * 根据模板名称生成ID
     *
     * @param templateName 模板名称
     * @return ID字符串
     */
    public static String nextIdByTemplate(String templateName) {
        if (templateGenerator == null) {
            throw new IdGeneratorException("IdTemplateGenerator未初始化");
        }
        return templateGenerator.nextId(templateName);
    }

    /**
     * 根据业务标识创建一个简单模板并生成ID
     * 
     * @param businessKey 业务标识
     * @return 生成的ID
     */
    public static String nextIdForBusiness(String businessKey) {
        if (templateGenerator == null) {
            throw new IdGeneratorException("IdTemplateGenerator未初始化");
        }
        return templateGenerator.nextIdForBusiness(businessKey);
    }

    /**
     * 注册ID模板
     *
     * @param template ID模板
     */
    public static void registerTemplate(IdTemplate template) {
        if (templateGenerator == null) {
            throw new IdGeneratorException("IdTemplateGenerator未初始化");
        }
        templateGenerator.registerTemplate(template);
    }
}