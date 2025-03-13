package com.easy.cache.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 熔断器缓存装饰器，用于防止缓存雪崩
 */
public class CircuitBreakerCache<K, V> extends AbstractCache<K, V> {

    private final Cache<K, V> delegate;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final int failureThreshold;
    private final long resetTimeout;
    private final TimeUnit resetTimeUnit;
    private volatile long openTime;

    /**
     * 创建熔断器缓存
     * 
     * @param delegate 被装饰的缓存
     * @param failureThreshold 失败阈值
     * @param resetTimeout 重置超时时间
     * @param resetTimeUnit 时间单位
     */
    public CircuitBreakerCache(Cache<K, V> delegate, int failureThreshold, long resetTimeout, TimeUnit resetTimeUnit) {
        super(delegate.getName() + ":circuit-breaker");
        this.delegate = delegate;
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
        this.resetTimeUnit = resetTimeUnit;
    }

    @Override
    public V get(K key) {
        checkCircuitState();
        
        if (circuitOpen.get()) {
            // 熔断器打开，返回null
            return null;
        }
        
        try {
            V value = delegate.get(key);
            // 成功，重置失败计数
            failureCount.set(0);
            return value;
        } catch (Exception e) {
            // 失败，增加失败计数
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                // 打开熔断器
                openCircuit();
            }
            throw e;
        }
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        checkCircuitState();
        
        if (circuitOpen.get()) {
            // 熔断器打开，返回null
            return null;
        }
        
        try {
            V value = delegate.get(key, loader);
            // 成功，重置失败计数
            failureCount.set(0);
            return value;
        } catch (Exception e) {
            // 失败，增加失败计数
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                // 打开熔断器
                openCircuit();
            }
            throw e;
        }
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        checkCircuitState();
        
        if (circuitOpen.get()) {
            // 熔断器打开，不执行操作
            return;
        }
        
        try {
            delegate.put(key, value, expireTime, timeUnit);
            // 成功，重置失败计数
            failureCount.set(0);
        } catch (Exception e) {
            // 失败，增加失败计数
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                // 打开熔断器
                openCircuit();
            }
            throw e;
        }
    }

    @Override
    public boolean remove(K key) {
        checkCircuitState();
        
        if (circuitOpen.get()) {
            // 熔断器打开，不执行操作
            return false;
        }
        
        try {
            boolean result = delegate.remove(key);
            // 成功，重置失败计数
            failureCount.set(0);
            return result;
        } catch (Exception e) {
            // 失败，增加失败计数
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                // 打开熔断器
                openCircuit();
            }
            throw e;
        }
    }

    @Override
    public void clear() {
        checkCircuitState();
        
        if (circuitOpen.get()) {
            // 熔断器打开，不执行操作
            return;
        }
        
        try {
            delegate.clear();
            // 成功，重置失败计数
            failureCount.set(0);
        } catch (Exception e) {
            // 失败，增加失败计数
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                // 打开熔断器
                openCircuit();
            }
            throw e;
        }
    }

    /**
     * 检查熔断器状态
     */
    private void checkCircuitState() {
        if (circuitOpen.get()) {
            long now = System.currentTimeMillis();
            long openTimeMillis = resetTimeUnit.toMillis(resetTimeout);
            
            if (now - openTime >= openTimeMillis) {
                // 重置熔断器
                circuitOpen.set(false);
                failureCount.set(0);
            }
        }
    }

    /**
     * 打开熔断器
     */
    private void openCircuit() {
        circuitOpen.set(true);
        openTime = System.currentTimeMillis();
    }

    /**
     * 熔断器是否打开
     * 
     * @return 是否打开
     */
    public boolean isCircuitOpen() {
        return circuitOpen.get();
    }

    /**
     * 获取失败次数
     * 
     * @return 失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }
} 