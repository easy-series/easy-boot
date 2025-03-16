package com.easy.cache.config;

import java.time.Duration;
import java.util.function.Function;

import com.easy.cache.support.convertor.KeyConvertor;
import com.easy.cache.support.serializer.Serializer;
import com.easy.cache.support.sync.SyncStrategy;

import lombok.Builder;
import lombok.Data;

/**
 * 缓存配置类
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Data
@Builder
public class CacheConfig<K, V> {

    /**
     * 缓存名称
     */
    private String name;

    /**
     * 缓存类型
     */
    private CacheType cacheType;

    /**
     * 键转换器
     */
    private KeyConvertor keyConvertor;

    /**
     * 值序列化器
     */
    private Serializer valueSerializer;

    /**
     * 写入后过期时间
     */
    private Duration expireAfterWrite;

    /**
     * 访问后过期时间
     */
    private Duration expireAfterAccess;

    /**
     * 写入后刷新时间
     */
    private Duration refreshAfterWrite;

    /**
     * 缓存加载器
     */
    private Function<K, V> loader;

    /**
     * 初始容量
     */
    private int initialCapacity;

    /**
     * 本地缓存条目数量限制
     */
    private int localLimit;

    /**
     * 是否启用本地缓存同步
     */
    private boolean syncLocal;

    /**
     * 缓存同步策略
     */
    private SyncStrategy syncStrategy;

    /**
     * 是否缓存null值
     */
    private boolean cacheNullValues;

    /**
     * 是否启用缓存穿透保护
     */
    private boolean penetrationProtect;

    /**
     * 是否使用写透策略（先写远程，再写本地）
     */
    private boolean writeThrough;

    /**
     * 是否异步写入远程缓存
     */
    private boolean asyncWrite;

    /**
     * 是否记录缓存统计信息
     */
    private boolean recordStats;
}