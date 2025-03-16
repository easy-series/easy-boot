package com.easy.cache.support.convertor;

/**
 * 键转换器接口，用于转换缓存键
 */
public interface KeyConvertor {
    
    /**
     * 将对象转换为字符串形式的键
     *
     * @param key 原始键对象
     * @return 转换后的字符串形式的键
     */
    String convert(Object key);
}