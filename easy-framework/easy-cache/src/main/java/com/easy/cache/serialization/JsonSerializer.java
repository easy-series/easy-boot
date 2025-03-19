package com.easy.cache.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * JSON序列化器实现
 */
public class JsonSerializer implements Serializer {
    private final ObjectMapper objectMapper;

    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
        // 配置ObjectMapper以提高兼容性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    }

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            // 处理输入可能是String的情况
            if (bytes.length > 2 && bytes[0] == '"' && bytes[bytes.length - 1] == '"') {
                // 可能是被双引号包围的JSON字符串
                String jsonStr = new String(bytes);
                if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                    // 去掉外层的引号，并尝试解析内部的JSON
                    jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                    return objectMapper.readValue(jsonStr, TypeFactory.defaultInstance().constructType(clazz));
                }
            }

            return objectMapper.readValue(bytes, TypeFactory.defaultInstance().constructType(clazz));
        } catch (IOException e) {
            // 尝试作为普通字符串处理
            try {
                String str = new String(bytes);
                return objectMapper.readValue(str, TypeFactory.defaultInstance().constructType(clazz));
            } catch (IOException e2) {
                throw new RuntimeException("Failed to deserialize object: " + new String(bytes), e);
            }
        }
    }
}