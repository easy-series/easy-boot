package com.easy.cache.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 基于Jackson的JSON序列化器实现
 */
public class JsonSerializer implements Serializer {

    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     */
    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 使用自定义ObjectMapper的构造函数
     *
     * @param objectMapper 自定义的ObjectMapper
     */
    public JsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object serialize(Object object) {
        if (object == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public Object deserialize(Object data) {
        if (data == null) {
            return null;
        }
        
        if (!(data instanceof String)) {
            throw new IllegalArgumentException("Data must be String for JSON deserialization");
        }
        
        try {
            // 注意：这里无法直接反序列化为泛型类型，需要在外部处理
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }
    
    /**
     * 将JSON字符串反序列化为指定类型的对象
     *
     * @param json JSON字符串
     * @param clazz 目标类型
     * @param <T> 泛型参数
     * @return 反序列化后的对象
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON to " + clazz.getName(), e);
        }
    }
} 