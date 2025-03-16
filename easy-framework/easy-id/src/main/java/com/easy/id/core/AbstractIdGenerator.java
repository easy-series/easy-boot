package com.easy.id.core;

/**
 * 抽象ID生成器实现，提供基础实现
 *
 * @author 芋道源码
 */
public abstract class AbstractIdGenerator implements IdGenerator {

    /**
     * ID生成器名称
     */
    private final String name;

    /**
     * 构造函数
     *
     * @param name 生成器名称
     */
    public AbstractIdGenerator(String name) {
        this.name = name;
    }

    @Override
    public long[] nextId(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        
        // 批量获取ID的默认实现，子类可以重写此方法提供更高效的实现
        long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            ids[i] = nextId();
        }
        return ids;
    }

    @Override
    public String getName() {
        return name;
    }
} 