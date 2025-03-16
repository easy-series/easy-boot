package com.easy.cache.serialization;

/**
 * 缓存键转换器
 */
public interface KeyConvertor {
    
    /**
     * 将缓存键转换为字符串
     * 
     * @param key 缓存键
     * @return 字符串表示
     */
    String convert(Object key);
} 