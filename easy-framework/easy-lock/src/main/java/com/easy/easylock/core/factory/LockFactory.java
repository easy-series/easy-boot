package com.easy.easylock.core.factory;

import java.util.HashMap;
import java.util.Map;

import com.easy.easylock.core.LockExecutor;
import com.easy.easylock.exception.LockException;

/**
 * 锁工厂，用于创建和管理锁执行器
 */
public class LockFactory {

    /**
     * 锁执行器类型枚举
     */
    public enum LockType {
        /**
         * Redis实现
         */
        REDIS,

        /**
         * Zookeeper实现（预留）
         */
        ZOOKEEPER
    }

    /**
     * 锁执行器映射表
     */
    private final Map<LockType, LockExecutor> executorMap = new HashMap<>();

    /**
     * 默认锁类型
     */
    private LockType defaultType = LockType.REDIS;

    /**
     * 构造函数，需传入默认锁执行器
     *
     * @param defaultExecutor 默认锁执行器
     * @param defaultType     默认锁类型
     */
    public LockFactory(LockExecutor defaultExecutor, LockType defaultType) {
        this.executorMap.put(defaultType, defaultExecutor);
        this.defaultType = defaultType;
    }

    /**
     * 注册锁执行器
     *
     * @param type     锁类型
     * @param executor 锁执行器
     */
    public void registerExecutor(LockType type, LockExecutor executor) {
        executorMap.put(type, executor);
    }

    /**
     * 设置默认锁类型
     *
     * @param defaultType 默认锁类型
     */
    public void setDefaultType(LockType defaultType) {
        if (!executorMap.containsKey(defaultType)) {
            throw new LockException("未注册该类型的锁执行器: " + defaultType);
        }
        this.defaultType = defaultType;
    }

    /**
     * 获取默认锁执行器
     *
     * @return 锁执行器
     */
    public LockExecutor getExecutor() {
        return getExecutor(defaultType);
    }

    /**
     * 获取指定类型的锁执行器
     *
     * @param type 锁类型
     * @return 锁执行器
     */
    public LockExecutor getExecutor(LockType type) {
        LockExecutor executor = executorMap.get(type);
        if (executor == null) {
            throw new LockException("未注册该类型的锁执行器: " + type);
        }
        return executor;
    }
}