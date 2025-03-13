package com.easy.cache.core;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 布隆过滤器缓存装饰器，用于防止缓存穿透
 */
public class BloomFilterCache<K, V> extends AbstractCache<K, V> {

    private final Cache<K, V> delegate;
    private final BloomFilter<String> bloomFilter;

    /**
     * 创建布隆过滤器缓存
     * 
     * @param delegate 被装饰的缓存
     * @param expectedInsertions 预期插入数量
     * @param fpp 误判率
     */
    public BloomFilterCache(Cache<K, V> delegate, int expectedInsertions, double fpp) {
        super(delegate.getName() + ":bloom");
        this.delegate = delegate;
        this.bloomFilter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()),
            expectedInsertions,
            fpp
        );
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }
        
        // 如果布隆过滤器中不存在，则直接返回null
        if (!bloomFilter.mightContain(key.toString())) {
            return null;
        }
        
        // 否则从缓存中获取
        return delegate.get(key);
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        if (key == null) {
            return null;
        }
        
        // 如果布隆过滤器中不存在，且有加载器，则尝试加载
        if (!bloomFilter.mightContain(key.toString()) && loader != null) {
            V value = loader.apply(key);
            if (value != null) {
                put(key, value);
            }
            return value;
        }
        
        // 否则从缓存中获取
        return delegate.get(key, loader);
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (key == null) {
            return;
        }
        
        delegate.put(key, value, expireTime, timeUnit);
        
        // 将键添加到布隆过滤器
        bloomFilter.put(key.toString());
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }
        
        // 注意：布隆过滤器不支持删除操作
        // 所以这里只从缓存中删除，布隆过滤器中的记录会保留
        return delegate.remove(key);
    }

    @Override
    public void clear() {
        delegate.clear();
        // 注意：布隆过滤器不支持清空操作
        // 这里需要重新创建一个布隆过滤器
        // 但这可能会导致内存泄漏，所以不建议频繁调用clear方法
    }
} 