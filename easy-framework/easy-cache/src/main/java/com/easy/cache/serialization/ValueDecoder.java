package com.easy.cache.serialization;

/**
 * 缓存值解码器
 * 
 * @param <T> 值类型
 */
public interface ValueDecoder<T> {
    
    /**
     * 将字节数组解码为缓存值对象
     * 
     * @param bytes 字节数组
     * @param clazz 目标对象类型
     * @return 缓存值对象
     */
    T decode(byte[] bytes, Class<T> clazz);
} 