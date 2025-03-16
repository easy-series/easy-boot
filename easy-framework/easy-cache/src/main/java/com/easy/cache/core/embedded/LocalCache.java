package com.easy.cache.core.embedded;

import com.easy.cache.api.Cache;

/**
 * 本地缓存接口，扩展基本缓存接口
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface LocalCache<K, V> extends Cache<K, V> {

    /**
     * 使指定键的缓存条目失效
     *
     * @param key 缓存键的字符串形式
     */
    void invalidate(String key);

    /**
     * 使指定键的缓存条目失效（别名方法）
     *
     * @param key 缓存键的字符串形式
     */
    default void evict(String key) {
        invalidate(key);
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存条目数量
     */
    long size();
}