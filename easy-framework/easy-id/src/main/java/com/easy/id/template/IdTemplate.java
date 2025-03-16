package com.easy.id.template;

import com.easy.id.core.IdGenerator;
import com.easy.id.exception.IdGeneratorException;
import com.easy.id.monitor.MonitoredIdGenerator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ID模板类，类似于RestTemplate，提供便捷的ID获取方法
 *
 * @author 芋道源码
 */
@Slf4j
public class IdTemplate {

    /**
     * 默认的ID生成器名称
     */
    private static final String DEFAULT_GENERATOR = "default";

    /**
     * ID生成器映射表
     */
    private final Map<String, IdGenerator> generators = new ConcurrentHashMap<>();

    /**
     * 默认的ID生成器
     */
    private IdGenerator defaultGenerator;

    /**
     * 构造函数
     *
     * @param defaultGenerator 默认ID生成器
     */
    public IdTemplate(IdGenerator defaultGenerator) {
        if (defaultGenerator != null) {
            // 如果默认生成器未被监控，则添加监控
            if (!(defaultGenerator instanceof MonitoredIdGenerator)) {
                defaultGenerator = new MonitoredIdGenerator(defaultGenerator);
            }
            this.defaultGenerator = defaultGenerator;
            this.generators.put(DEFAULT_GENERATOR, defaultGenerator);
        }
    }

    /**
     * 获取下一个ID（使用默认生成器）
     *
     * @return ID
     */
    public long nextId() {
        if (defaultGenerator == null) {
            throw new IdGeneratorException("未配置默认ID生成器");
        }
        return defaultGenerator.nextId();
    }

    /**
     * 批量获取ID（使用默认生成器）
     *
     * @param count ID数量
     * @return ID数组
     */
    public long[] nextId(int count) {
        if (defaultGenerator == null) {
            throw new IdGeneratorException("未配置默认ID生成器");
        }
        return defaultGenerator.nextId(count);
    }

    /**
     * 使用指定生成器获取下一个ID
     *
     * @param generatorName 生成器名称
     * @return ID
     */
    public long nextId(String generatorName) {
        IdGenerator generator = getGenerator(generatorName);
        return generator.nextId();
    }

    /**
     * 使用指定生成器批量获取ID
     *
     * @param generatorName 生成器名称
     * @param count         ID数量
     * @return ID数组
     */
    public long[] nextId(String generatorName, int count) {
        IdGenerator generator = getGenerator(generatorName);
        return generator.nextId(count);
    }

    /**
     * 获取指定名称的生成器
     *
     * @param generatorName 生成器名称
     * @return 生成器
     */
    private IdGenerator getGenerator(String generatorName) {
        IdGenerator generator = generators.get(generatorName);
        if (generator == null) {
            throw new IdGeneratorException("未找到名为[" + generatorName + "]的ID生成器");
        }
        return generator;
    }

    /**
     * 注册ID生成器
     *
     * @param name      生成器名称
     * @param generator 生成器
     */
    public void registerGenerator(String name, IdGenerator generator) {
        // 如果生成器未被监控，则添加监控
        if (!(generator instanceof MonitoredIdGenerator)) {
            generator = new MonitoredIdGenerator(generator);
        }
        generators.put(name, generator);

        // 如果尚未设置默认生成器，则设置为默认
        if (defaultGenerator == null) {
            defaultGenerator = generator;
            generators.put(DEFAULT_GENERATOR, generator);
        }

        log.info("注册ID生成器: {}", name);
    }

    /**
     * 设置默认生成器
     *
     * @param generator 生成器
     */
    public void setDefaultGenerator(IdGenerator generator) {
        // 如果生成器未被监控，则添加监控
        if (!(generator instanceof MonitoredIdGenerator)) {
            generator = new MonitoredIdGenerator(generator);
        }
        this.defaultGenerator = generator;
        this.generators.put(DEFAULT_GENERATOR, generator);
        log.info("设置默认ID生成器: {}", generator.getName());
    }

    /**
     * 获取所有注册的生成器
     *
     * @return 生成器映射表
     */
    public Map<String, IdGenerator> getGenerators() {
        return new ConcurrentHashMap<>(generators);
    }
}