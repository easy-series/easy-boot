package com.easy.cache.core;

/**
 * 结果数据，用于异步操作返回
 * 
 * @param <V> 数据类型
 */
public class ResultData<V> {
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 结果代码
     */
    private int resultCode;
    
    /**
     * 结果数据
     */
    private V data;
    
    /**
     * 创建成功的结果数据
     * 
     * @param data 数据
     * @param <V> 数据类型
     * @return 结果数据
     */
    public static <V> ResultData<V> success(V data) {
        ResultData<V> result = new ResultData<>();
        result.success = true;
        result.resultCode = ResultCode.SUCCESS;
        result.data = data;
        return result;
    }
    
    /**
     * 创建失败的结果数据
     * 
     * @param resultCode 结果代码
     * @param <V> 数据类型
     * @return 结果数据
     */
    public static <V> ResultData<V> fail(int resultCode) {
        ResultData<V> result = new ResultData<>();
        result.success = false;
        result.resultCode = resultCode;
        return result;
    }
    
    /**
     * 检查操作是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取结果代码
     * 
     * @return 结果代码
     */
    public int getResultCode() {
        return resultCode;
    }
    
    /**
     * 获取结果数据
     * 
     * @return 结果数据
     */
    public V getData() {
        return data;
    }
} 