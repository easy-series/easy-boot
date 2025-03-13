package com.easy.cache.support;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 默认的缓存键生成器
 */
public class DefaultKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length == 0) {
            return generateKey(method.getName());
        }
        if (params.length == 1) {
            Object param = params[0];
            if (param != null && !param.getClass().isArray()) {
                return generateKey(method.getName(), param);
            }
        }
        return generateKey(method.getName(), params);
    }

    /**
     * 生成缓存键
     */
    private String generateKey(String methodName, Object... params) {
        StringBuilder sb = new StringBuilder(methodName);
        sb.append(":");

        for (Object param : params) {
            sb.append(generateKeyPart(param)).append("_");
        }

        if (sb.charAt(sb.length() - 1) == '_') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * 生成键的一部分
     */
    private String generateKeyPart(Object param) {
        if (param == null) {
            return "null";
        }
        if (param.getClass().isArray()) {
            return Arrays.deepToString((Object[]) param);
        }
        return param.toString();
    }
}