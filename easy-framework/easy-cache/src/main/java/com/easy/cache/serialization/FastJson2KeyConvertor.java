package com.easy.cache.serialization;

import com.alibaba.fastjson2.JSON;

/**
 * 基于FastJson2的键转换器
 */
public class FastJson2KeyConvertor implements KeyConvertor {
    
    @Override
    public String convert(Object key) {
        if (key == null) {
            return "null";
        }
        
        if (key instanceof String) {
            return (String) key;
        }
        
        if (key instanceof Number || key instanceof Boolean || key instanceof Character) {
            return key.toString();
        }
        
        // 使用FastJson2序列化对象
        return JSON.toJSONString(key);
    }
} 