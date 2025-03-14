package com.easy.cache.core;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 布隆过滤器缓存实现，用于防止缓存穿透
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class BloomFilterCache<K, V> extends AbstractCache<K, V> {

    /**
     * 被装饰的缓存
     */
    private final Cache<K, V> delegate;

    /**
     * 布隆过滤器
     */
    private final BloomFilter<String> bloomFilter;

    /**
     * 空值标记
     */
    private final V nullValue;

    /**
     * 构造函数
     *
     * @param delegate 被装饰的缓存
     * @param expectedInsertions 预期插入元素数量
     * @param fpp 误判率
     * @param nullValue 空值标记，用于缓存不存在的值
     */
    public BloomFilterCache(Cache<K, V> delegate, int expectedInsertions, double fpp, V nullValue) {
        super(delegate.getName() + "_bloom_filter");
        this.delegate = delegate;
        this.bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                fpp);
        this.nullValue = nullValue;
    }

    /**
     * 将键转换为字符串
     *
     * @param key 键
     * @return 字符串键
     */
    private String keyToString(K key) {
        return key == null ? "null" : key.toString();
    }

    @Override
    public V get(K key) {
        // 如果布隆过滤器判断键不存在，直接返回空
        if (!bloomFilter.mightContain(keyToString(key))) {
            return null;
        }

        // 从委托缓存获取值
        V value = delegate.get(key);

        // 如果获取的是空值标记，返回null
        if (value != null && value.equals(nullValue)) {
            return null;
        }

        return value;
    }

    @Override
    public void put(K key, V value, long expire, TimeUnit timeUnit) {
        // 将键添加到布隆过滤器
        bloomFilter.put(keyToString(key));

        // 如果值为null，使用空值标记代替
        if (value == null && nullValue != null) {
            delegate.put(key, nullValue, expire, timeUnit);
        } else {
            delegate.put(key, value, expire, timeUnit);
        }
    }

    @Override
    public boolean remove(K key) {
        // 从委托缓存移除，但不从布隆过滤器移除（布隆过滤器不支持删除）
        return delegate.remove(key);
    }

    @Override
    public void clear() {
        // 只清除委托缓存，无法清除布隆过滤器
        delegate.clear();
    }

    @Override
    public boolean contains(K key) {
        // 先检查布隆过滤器
        if (!bloomFilter.mightContain(keyToString(key))) {
            return false;
        }
        // 再检查委托缓存
        return delegate.contains(key);
    }

    @Override
    public long size() {
        return delegate.size();
    }
} 