package com.easy.cache.support.serializer;

/**
 * 序列化器接口，用于序列化缓存值
 */
public interface Serializer {
    
    /**
     * 将对象序列化为字节数组
     *
     * @param obj 要序列化的对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object obj);
    
    /**
     * 将字节数组反序列化为对象
     *
     * @param bytes 要反序列化的字节数组
     * @param clazz 目标类型
     * @param <T> 目标类型参数
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}