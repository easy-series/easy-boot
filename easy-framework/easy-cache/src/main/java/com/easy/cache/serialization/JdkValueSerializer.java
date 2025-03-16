package com.easy.cache.serialization;

import com.easy.cache.exception.CacheException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 基于JDK的值序列化器，同时实现编码器和解码器
 */
public class JdkValueSerializer implements ValueEncoder, ValueDecoder<Object> {
    
    @Override
    public byte[] encode(Object value) {
        if (value == null) {
            return new byte[0];
        }
        
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            
            oos.writeObject(value);
            oos.flush();
            return bos.toByteArray();
            
        } catch (IOException e) {
            throw new CacheException("JDK序列化失败", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            
            return (T) ois.readObject();
            
        } catch (IOException | ClassNotFoundException e) {
            throw new CacheException("JDK反序列化失败", e);
        }
    }
} 