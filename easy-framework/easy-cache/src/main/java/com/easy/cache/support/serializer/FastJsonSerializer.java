package com.easy.cache.support.serializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于FastJson2的序列化器实现
 */
@Slf4j
public class FastJsonSerializer implements Serializer {

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }

        try {
            return JSON.toJSONBytes(obj, JSONWriter.Feature.WriteClassName);
        } catch (Exception e) {
            log.error("序列化对象失败: obj={}", obj, e);
            throw new RuntimeException("序列化对象失败", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            return (T) JSON.parseObject(bytes, Object.class, JSONReader.Feature.SupportAutoType);
        } catch (Exception e) {
            log.error("反序列化对象失败: clazz={}", clazz.getName(), e);
            throw new RuntimeException("反序列化对象失败", e);
        }
    }
}