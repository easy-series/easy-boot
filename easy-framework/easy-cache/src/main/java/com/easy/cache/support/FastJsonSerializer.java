package com.easy.cache.support;

import com.alibaba.fastjson2.JSON;
import com.easy.cache.core.RedisCache.Serializer;

/**
 * FastJson 序列化器
 */
public class FastJsonSerializer implements Serializer {

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }

        String jsonString = JSON.toJSONString(obj);
        return jsonString.getBytes();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        String jsonString = new String(bytes);
        return JSON.parseObject(jsonString, clazz);
    }
}