package com.easy.cache.security;

/**
 * 加密器接口
 */
public interface Encryptor {
    
    /**
     * 加密数据
     * 
     * @param data 原始数据
     * @return 加密后的数据
     * @throws Exception 加密异常
     */
    byte[] encrypt(byte[] data) throws Exception;
    
    /**
     * 解密数据
     * 
     * @param data 加密数据
     * @return 解密后的数据
     * @throws Exception 解密异常
     */
    byte[] decrypt(byte[] data) throws Exception;
} 