package com.easy.id.monitor;

import com.easy.id.core.IdGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 带监控功能的ID生成器包装类
 *
 * 为ID生成器添加监控功能，记录生成耗时和成功/失败次数
 *
 * @author 芋道源码
 */
@Slf4j
@Data
public class MonitoredIdGenerator implements IdGenerator {

    /**
     * 被包装的ID生成器
     */
    private final IdGenerator delegate;

    /**
     * 监控器
     */
    private final IdGeneratorMonitor monitor;

    /**
     * 构造函数
     *
     * @param delegate 被包装的ID生成器
     */
    public MonitoredIdGenerator(IdGenerator delegate) {
        this.delegate = delegate;
        this.monitor = IdGeneratorMonitor.getInstance();
        this.monitor.register(delegate);
    }

    @Override
    public long nextId() {
        long startTime = System.currentTimeMillis();
        try {
            long id = delegate.nextId();
            monitor.recordSuccess(delegate.getName());
            return id;
        } catch (Exception e) {
            monitor.recordFailure(delegate.getName());
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            monitor.recordTime(delegate.getName(), costTime);
        }
    }

    @Override
    public long[] nextId(int count) {
        long startTime = System.currentTimeMillis();
        try {
            long[] ids = delegate.nextId(count);
            monitor.recordSuccess(delegate.getName());
            return ids;
        } catch (Exception e) {
            monitor.recordFailure(delegate.getName());
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            monitor.recordTime(delegate.getName(), costTime);
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    /**
     * 获取被包装的ID生成器
     *
     * @return 原始的ID生成器
     */
    public IdGenerator getDelegate() {
        return delegate;
    }
}