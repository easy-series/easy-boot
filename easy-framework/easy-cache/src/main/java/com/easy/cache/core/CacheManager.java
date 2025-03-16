package com.easy.cache.core;

import com.easy.cache.config.CacheConfig;
import com.easy.cache.config.QuickConfig;

/**
 * 缓存管理器接口，用于获取和创建缓存实例
 */
public interface CacheManager {
    
    /**
     * 获取指定名称的缓存
     * 
     * @param name 缓存名称
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 缓存实例，如果不存在则返回null
     */
    <K, V> Cache<K, V> getCache(String name);
    
    /**
     * 获取或创建缓存
     * 
     * @param config 缓存配置
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 缓存实例
     */
    <K, V> Cache<K, V> getOrCreateCache(CacheConfig<K, V> config);
    
    /**
     * 获取或创建缓存（使用快速配置）
     * 
     * @param config 快速配置
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 缓存实例
     */
    <K, V> Cache<K, V> getOrCreateCache(QuickConfig config);
    
    /**
     * 移除缓存
     * 
     * @param name 缓存名称
     * @return 是否成功移除
     */
    boolean removeCache(String name);
    
    /**
     * 获取管理器名称
     * 
     * @return 管理器名称
     */
    String getName();
} 