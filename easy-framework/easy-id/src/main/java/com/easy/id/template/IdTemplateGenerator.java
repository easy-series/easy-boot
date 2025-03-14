package com.easy.id.template;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.easy.id.core.IdGenerator;
import com.easy.id.exception.IdGeneratorException;

import lombok.extern.slf4j.Slf4j;

/**
 * ID模板生成器
 * 根据模板定义生成ID
 */
@Slf4j
@Component
public class IdTemplateGenerator implements InitializingBean {

    @Autowired
    private IdGenerator idGenerator;

    /**
     * 模板缓存
     */
    private final Map<String, IdTemplate> templateMap = new ConcurrentHashMap<>();

    /**
     * 序列号计数器（用于非雪花算法模式）
     */
    private final Map<String, AtomicLong> sequenceMap = new ConcurrentHashMap<>();

    /**
     * 日期缓存
     */
    private final Map<String, String> dateCache = new ConcurrentHashMap<>();

    /**
     * 最后更新日期时间戳
     */
    private final Map<String, Long> lastDateUpdateMap = new ConcurrentHashMap<>();

    @Override
    public void afterPropertiesSet() {
        log.info("ID模板生成器初始化完成");
    }

    /**
     * 根据模板名称生成ID
     *
     * @param templateName 模板名称
     * @return 生成的ID
     */
    public String nextId(String templateName) {
        IdTemplate template = templateMap.get(templateName);
        if (template == null) {
            throw new IdGeneratorException("模板不存在: " + templateName);
        }
        return generateId(template);
    }

    /**
     * 根据业务标识创建一个简单模板并生成ID
     * 
     * @param businessKey 业务标识
     * @return 生成的ID
     */
    public String nextIdForBusiness(String businessKey) {
        if (StringUtils.isEmpty(businessKey)) {
            throw new IdGeneratorException("业务标识不能为空");
        }

        String templateName = "auto_" + businessKey;
        if (!templateExists(templateName)) {
            IdTemplate template = IdTemplate.simple(businessKey);
            template.setName(templateName);
            registerTemplate(template);
        }

        return nextId(templateName);
    }

    /**
     * 根据业务标识直接使用雪花算法生成ID
     * 
     * @param businessKey 业务标识
     * @return 生成的ID
     */
    public long nextSnowflakeId(String businessKey) {
        return idGenerator.nextId(businessKey);
    }

    /**
     * 注册ID模板
     *
     * @param template ID模板
     */
    public void registerTemplate(IdTemplate template) {
        if (template == null) {
            throw new IdGeneratorException("模板不能为空");
        }

        if (StringUtils.isEmpty(template.getName())) {
            throw new IdGeneratorException("模板名称不能为空");
        }

        templateMap.put(template.getName(), template);
        sequenceMap.putIfAbsent(template.getName(), new AtomicLong(0));
        log.info("注册ID模板: {}", template.getName());
    }

    /**
     * 批量注册ID模板
     * 
     * @param templates 模板列表
     */
    public void registerTemplates(IdTemplate... templates) {
        if (templates != null) {
            for (IdTemplate template : templates) {
                registerTemplate(template);
            }
        }
    }

    /**
     * 根据模板生成ID
     *
     * @param template ID模板
     * @return 生成的ID
     */
    private String generateId(IdTemplate template) {
        if (template.isUseSnowflake()) {
            return generateSnowflakeId(template);
        } else {
            return generateCustomId(template);
        }
    }

    /**
     * 生成雪花算法ID
     */
    private String generateSnowflakeId(IdTemplate template) {
        long id = idGenerator.nextId(template.getBusinessKey());

        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(template.getPrefix())) {
            sb.append(template.getPrefix());
            if (!StringUtils.isEmpty(template.getSeparator())) {
                sb.append(template.getSeparator());
            }
        }

        sb.append(id);
        return sb.toString();
    }

    /**
     * 生成自定义ID
     */
    private String generateCustomId(IdTemplate template) {
        StringBuilder sb = new StringBuilder();

        // 添加前缀
        if (!StringUtils.isEmpty(template.getPrefix())) {
            sb.append(template.getPrefix());
            if (!StringUtils.isEmpty(template.getSeparator())) {
                sb.append(template.getSeparator());
            }
        }

        // 添加日期部分
        if (!StringUtils.isEmpty(template.getDateFormat())) {
            String dateStr = getFormattedDate(template);
            sb.append(dateStr);
            if (!StringUtils.isEmpty(template.getSeparator())) {
                sb.append(template.getSeparator());
            }
        }

        // 添加序列号部分
        long sequence = getNextSequence(template);
        String sequenceStr = String.valueOf(sequence);
        if (template.getSequenceLength() > 0 && sequenceStr.length() < template.getSequenceLength()) {
            // 左填充0
            sb.append(String.format("%0" + template.getSequenceLength() + "d", sequence));
        } else {
            sb.append(sequence);
        }

        return sb.toString();
    }

    /**
     * 获取下一个序列号
     */
    private long getNextSequence(IdTemplate template) {
        AtomicLong counter = sequenceMap.computeIfAbsent(template.getName(), k -> new AtomicLong(0));

        // 检查是否需要重置计数器（日期变更时）
        if (!StringUtils.isEmpty(template.getDateFormat())) {
            String currentDate = getFormattedDate(template);
            String lastDate = dateCache.get(template.getName());

            if (lastDate == null || !lastDate.equals(currentDate)) {
                counter.set(0);
                dateCache.put(template.getName(), currentDate);
            }
        }

        // 检查是否达到最大值
        if (template.getMaxSequence() > 0 && counter.get() >= template.getMaxSequence()) {
            throw new IdGeneratorException("序列号超过最大值: " + template.getMaxSequence());
        }

        return counter.incrementAndGet();
    }

    /**
     * 获取格式化后的日期
     */
    private String getFormattedDate(IdTemplate template) {
        String key = template.getName() + "_" + template.getDateFormat();
        long now = System.currentTimeMillis();
        Long lastUpdate = lastDateUpdateMap.getOrDefault(key, 0L);

        // 缓存1秒钟，避免频繁创建SimpleDateFormat
        if (now - lastUpdate < 1000 && dateCache.containsKey(key)) {
            return dateCache.get(key);
        }

        SimpleDateFormat sdf = new SimpleDateFormat(template.getDateFormat());
        String dateStr = sdf.format(new Date());

        dateCache.put(key, dateStr);
        lastDateUpdateMap.put(key, now);

        return dateStr;
    }

    /**
     * 判断模板是否存在
     */
    public boolean templateExists(String templateName) {
        return templateMap.containsKey(templateName);
    }

    /**
     * 获取模板
     */
    public IdTemplate getTemplate(String templateName) {
        return templateMap.get(templateName);
    }

    /**
     * 删除模板
     */
    public void removeTemplate(String templateName) {
        templateMap.remove(templateName);
        sequenceMap.remove(templateName);
        log.info("删除ID模板: {}", templateName);
    }
}