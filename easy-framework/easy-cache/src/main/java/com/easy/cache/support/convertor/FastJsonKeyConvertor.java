package com.easy.cache.support.convertor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于FastJson2的键转换器实现
 */
@Slf4j
public class FastJsonKeyConvertor implements KeyConvertor {

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

        try {
            return JSON.toJSONString(key, JSONWriter.Feature.WriteClassName);
        } catch (Exception e) {
            log.error("转换键失败: key={}", key, e);
            // 回退到toString方法
            return key.toString();
        }
    }
}