package com.easy.cache.api;

import com.easy.cache.config.CacheType;
import com.easy.cache.support.convertor.KeyConvertor;
import com.easy.cache.support.serializer.Serializer;
import com.easy.cache.support.sync.SyncStrategy;

import java.time.Duration;
import java.util.function.Function;

/**
 * 缓存构建器接口，用于构建缓存实例
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface CacheBuilder<K, V> {

    /**
     * 设置缓存名称
     *
     * @param name 缓存名称
     * @return 当前构建器
     */
    CacheBuilder<K, V> name(String name);

    /**
     * 设置键转换器
     *
     * @param keyConvertor 键转换器
     * @return 当前构建器
     */
    CacheBuilder<K, V> keyConvertor(KeyConvertor keyConvertor);

    /**
     * 设置值序列化器
     *
     * @param valueSerializer 值序列化器
     * @return 当前构建器
     */
    CacheBuilder<K, V> valueSerializer(Serializer valueSerializer);

    /**
     * it设置写入后过期时间
     *
     * @param duration 过期时间
     * @return 当前构建器
     */
    CacheBuilder<K, V> expireAfterWrite(Duration duration);

    /**
     * 设置访问后过期时间
     *
     * @param duration 过期时间
     * @return 当前构建器
     */
    CacheBuilder<K, V> expireAfterAccess(Duration duration);

    /**
     * 设置缓存加载器
     *
     * @param loader 缓存加载器
     * @return 当前构建器
     */
    CacheBuilder<K, V> loader(Function<K, V> loader);

    /**
     * 设置缓存类型
     *
     * @param cacheType 缓存类型
     * @return 当前构建器
     */
    CacheBuilder<K, V> cacheType(CacheType cacheType);

    /**
     * 设置本地缓存条目数量限制
     *
     * @param limit 限制数量
     * @return 当前构建器
     */
    CacheBuilder<K, V> localLimit(int limit);

    /**
     * 设置是否启用本地缓存同步
     *
     * @param syncLocal 是否启用本地缓存同步
     * @return 当前构建器
     */
    CacheBuilder<K, V> syncLocal(boolean syncLocal);

    /**
     * 设置缓存同步策略
     *
     * @param strategy 同步策略
     * @return 当前构建器
     */
    CacheBuilder<K, V> syncStrategy(SyncStrategy strategy);

    /**
     * 设置是否缓存null值
     *
     * @param cacheNullValues 是否缓存null值
     * @return 当前构建器
     */
    CacheBuilder<K, V> cacheNullValues(boolean cacheNullValues);

    /**
     * 设置是否启用缓存穿透保护
     *
     * @param penetrationProtect 是否启用缓存穿透保护
     * @return 当前构建器
     */
    CacheBuilder<K, V> penetrationProtect(boolean penetrationProtect);

    /**
     * 构建缓存实例
     *
     * @return 缓存实例
     */
    Cache<K, V> build();
}