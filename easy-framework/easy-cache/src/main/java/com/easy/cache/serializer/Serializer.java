package com.easy.cache.serializer;

/**
 * 缓存序列化接口
 */
public interface Serializer {

    /**
     * 将对象序列化
     *
     * @param object 要序列化的对象
     * @return 序列化后的对象
     */
    Object serialize(Object object);

    /**
     * 将序列化的数据反序列化为对象
     *
     * @param data 序列化的数据
     * @return 反序列化后的对象
     */
    Object deserialize(Object data);
} 