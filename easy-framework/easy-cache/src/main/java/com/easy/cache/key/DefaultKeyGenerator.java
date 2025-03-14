package com.easy.cache.key;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 默认缓存键生成器实现
 */
public class DefaultKeyGenerator implements KeyGenerator {

    @Override
    public String generate(Object target, Method method, Object... params) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 添加类名
        keyBuilder.append(target.getClass().getSimpleName());
        keyBuilder.append(".");
        
        // 添加方法名
        keyBuilder.append(method.getName());
        
        // 添加参数
        if (params != null && params.length > 0) {
            keyBuilder.append(":");
            keyBuilder.append(generateKeyFromParams(params));
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 从参数生成键
     *
     * @param params 方法参数
     * @return 键字符串
     */
    protected String generateKeyFromParams(Object... params) {
        if (params.length == 1) {
            Object param = params[0];
            if (param == null) {
                return "null";
            }
            
            if (param instanceof String || param instanceof Number || param instanceof Boolean) {
                return param.toString();
            }
        }
        
        return Arrays.deepHashCode(params) + "";
    }
} 