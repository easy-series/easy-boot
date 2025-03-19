package com.easy.cache.serialization;

/**
 * 序列化器接口
 */
public interface Serializer {
    /**
     * 序列化对象
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}