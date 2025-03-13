package com.easy.cache.support;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpEL表达式解析器
 */
public class SpelExpressionParser {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\#\\{([^\\}]+)\\}");
    private static final Map<String, ExpressionExecutor> EXECUTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * 解析SpEL表达式
     * 
     * @param expression 表达式
     * @param method     方法
     * @param args       方法参数
     * @param target     目标对象
     * @return 解析结果
     */
    public static Object parseExpression(String expression, Method method, Object[] args, Object target) {
        if (expression == null || expression.isEmpty()) {
            return expression;
        }

        // 如果不包含表达式，直接返回
        if (!expression.contains("#{")) {
            return expression;
        }

        // 创建上下文
        Map<String, Object> context = createContext(method, args, target);

        // 替换所有表达式
        StringBuffer result = new StringBuffer();
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);

        while (matcher.find()) {
            String expressionContent = matcher.group(1);
            Object value = evaluateExpression(expressionContent, context);
            matcher.appendReplacement(result, value == null ? "null" : value.toString());
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 创建表达式上下文
     */
    private static Map<String, Object> createContext(Method method, Object[] args, Object target) {
        Map<String, Object> context = new HashMap<>();

        // 添加方法参数
        String[] paramNames = getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            // 按索引添加参数
            context.put("p" + i, args[i]);

            // 按名称添加参数（如果有参数名）
            if (paramNames != null && i < paramNames.length) {
                context.put(paramNames[i], args[i]);
            }
        }

        // 添加方法信息
        context.put("method", method);
        context.put("methodName", method.getName());
        context.put("target", target);
        context.put("targetClass", target.getClass());
        context.put("args", args);

        return context;
    }

    /**
     * 获取方法参数名
     * 注意：这里简化处理，实际项目中可以使用ASM或反射获取真实参数名
     */
    private static String[] getParameterNames(Method method) {
        // 这里简化处理，实际项目中可以使用ASM或反射获取真实参数名
        return null;
    }

    /**
     * 执行表达式
     */
    private static Object evaluateExpression(String expression, Map<String, Object> context) {
        // 从缓存获取表达式执行器
        ExpressionExecutor executor = EXECUTOR_CACHE.computeIfAbsent(expression, SpelExpressionParser::createExecutor);
        return executor.execute(context);
    }

    /**
     * 创建表达式执行器
     */
    private static ExpressionExecutor createExecutor(String expression) {
        // 简单变量引用，如 #p0, #args[0], #user.name
        if (expression.startsWith("#")) {
            return createVariableExecutor(expression.substring(1));
        }

        // 方法调用，如 target.getClass().getSimpleName()
        if (expression.contains(".")) {
            return createMethodExecutor(expression);
        }

        // 常量表达式
        return context -> expression;
    }

    /**
     * 创建变量引用执行器
     */
    private static ExpressionExecutor createVariableExecutor(String variable) {
        // 数组或集合访问，如 args[0]
        if (variable.contains("[")) {
            return createArrayAccessExecutor(variable);
        }

        // 属性访问，如 user.name
        if (variable.contains(".")) {
            return createPropertyAccessExecutor(variable);
        }

        // 简单变量，如 p0, methodName
        return context -> context.get(variable);
    }

    /**
     * 创建数组访问执行器
     */
    private static ExpressionExecutor createArrayAccessExecutor(String expression) {
        int bracketIndex = expression.indexOf('[');
        String arrayName = expression.substring(0, bracketIndex);
        String indexStr = expression.substring(bracketIndex + 1, expression.indexOf(']'));

        return context -> {
            Object array = context.get(arrayName);
            if (array == null) {
                return null;
            }

            int index;
            try {
                index = Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                return null;
            }

            if (array instanceof Object[]) {
                Object[] objArray = (Object[]) array;
                return index >= 0 && index < objArray.length ? objArray[index] : null;
            } else if (array instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) array;
                return index >= 0 && index < list.size() ? list.get(index) : null;
            }

            return null;
        };
    }

    /**
     * 创建属性访问执行器
     */
    private static ExpressionExecutor createPropertyAccessExecutor(String expression) {
        String[] parts = expression.split("\\.", 2);
        String objectName = parts[0];
        String propertyPath = parts[1];

        return context -> {
            Object obj = context.get(objectName);
            if (obj == null) {
                return null;
            }

            return getProperty(obj, propertyPath);
        };
    }

    /**
     * 创建方法调用执行器
     */
    private static ExpressionExecutor createMethodExecutor(String expression) {
        // 简化处理，实际项目中可以实现方法调用的解析
        return context -> expression;
    }

    /**
     * 获取对象属性值
     */
    private static Object getProperty(Object obj, String propertyPath) {
        if (obj == null || propertyPath == null || propertyPath.isEmpty()) {
            return null;
        }

        String[] parts = propertyPath.split("\\.", 2);
        String property = parts[0];

        try {
            // 尝试获取getter方法
            String getterName = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
            Method getter = obj.getClass().getMethod(getterName);
            Object value = getter.invoke(obj);

            // 如果还有更深层次的属性，递归获取
            if (parts.length > 1) {
                return getProperty(value, parts[1]);
            }

            return value;
        } catch (Exception e) {
            try {
                // 尝试获取boolean类型的is方法
                String isName = "is" + property.substring(0, 1).toUpperCase() + property.substring(1);
                Method isMethod = obj.getClass().getMethod(isName);
                Object value = isMethod.invoke(obj);

                // 如果还有更深层次的属性，递归获取
                if (parts.length > 1) {
                    return getProperty(value, parts[1]);
                }

                return value;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 表达式执行器接口
     */
    @FunctionalInterface
    private interface ExpressionExecutor {
        Object execute(Map<String, Object> context);
    }
}