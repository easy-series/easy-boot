package com.easy.cache.util;

/**
 * 序列化器接口
 */
public interface Serializer {

    /**
     * 序列化对象为字节数组
     *
     * @param obj 要序列化的对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化字节数组为对象
     *
     * @param bytes 要反序列化的字节数组
     * @param clazz 目标类
     * @param <T>   目标类型
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}