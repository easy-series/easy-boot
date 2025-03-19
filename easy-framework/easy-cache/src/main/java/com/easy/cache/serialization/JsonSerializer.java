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

        // 更多配置以提高兼容性
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
    }

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }

        try {
            // 如果是字符串，直接返回字节
            if (obj instanceof String) {
                return ((String) obj).getBytes();
            }

            System.out.println("序列化对象: " + obj.getClass().getName());
            byte[] bytes = objectMapper.writeValueAsBytes(obj);
            System.out.println("序列化结果: " + new String(bytes));
            return bytes;
        } catch (JsonProcessingException e) {
            System.err.println("序列化对象失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            String str = new String(bytes);
            System.out.println("尝试反序列化: " + str + " 到类型: " + clazz.getName());

            // 如果需要的是字符串，并且输入已经是字符串，直接返回
            if (clazz == String.class) {
                // 如果包含双引号，可能需要去除
                if (str.startsWith("\"") && str.endsWith("\"")) {
                    str = str.substring(1, str.length() - 1);
                }
                return (T) str;
            }

            // 处理输入可能是String的情况
            if (bytes.length > 2 && bytes[0] == '"' && bytes[bytes.length - 1] == '"') {
                // 可能是被双引号包围的JSON字符串
                if (str.startsWith("\"") && str.endsWith("\"")) {
                    // 去掉外层的引号，并尝试解析内部的JSON
                    str = str.substring(1, str.length() - 1).replace("\\\"", "\"");
                    try {
                        return objectMapper.readValue(str, TypeFactory.defaultInstance().constructType(clazz));
                    } catch (Exception e) {
                        System.err.println("反序列化嵌套JSON失败: " + e.getMessage());
                    }
                }
            }

            // 尝试直接反序列化
            try {
                return objectMapper.readValue(bytes, TypeFactory.defaultInstance().constructType(clazz));
            } catch (Exception e) {
                System.err.println("直接反序列化失败，尝试字符串方式: " + e.getMessage());
                // 再次尝试字符串方式
                return objectMapper.readValue(str, TypeFactory.defaultInstance().constructType(clazz));
            }
        } catch (IOException e) {
            System.err.println("反序列化失败: " + e.getMessage() + " 的内容: " + new String(bytes));
            e.printStackTrace();
            throw new RuntimeException("Failed to deserialize object: " + new String(bytes), e);
        }
    }
}