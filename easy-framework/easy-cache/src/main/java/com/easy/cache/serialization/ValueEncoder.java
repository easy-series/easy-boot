package com.easy.cache.serialization;

/**
 * 缓存值编码器
 */
public interface ValueEncoder {
    
    /**
     * 将缓存值对象编码为字节数组
     * 
     * @param value 缓存值对象
     * @return 字节数组
     */
    byte[] encode(Object value);
} 