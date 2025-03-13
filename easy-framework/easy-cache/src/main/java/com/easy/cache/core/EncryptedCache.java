package com.easy.cache.core;

import com.easy.cache.security.Encryptor;
import org.apache.commons.lang3.SerializationUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 加密缓存装饰器，用于保护敏感数据
 */
public class EncryptedCache<K, V> extends AbstractCache<K, V> {
    
    private final Cache<K, byte[]> delegate;
    private final Encryptor encryptor;
    
    /**
     * 创建加密缓存
     * 
     * @param delegate 被装饰的缓存
     * @param encryptor 加密器
     */
    @SuppressWarnings("unchecked")
    public EncryptedCache(Cache<K, V> delegate, Encryptor encryptor) {
        super(delegate.getName() + ":encrypted");
        this.delegate = (Cache<K, byte[]>) delegate;
        this.encryptor = encryptor;
    }
    
    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }
        
        byte[] encryptedValue = delegate.get(key);
        if (encryptedValue == null) {
            return null;
        }
        
        try {
            byte[] decryptedValue = encryptor.decrypt(encryptedValue);
            @SuppressWarnings("unchecked")
            V value = (V) SerializationUtils.deserialize(decryptedValue);
            return value;
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
    
    @Override
    public V get(K key, Function<K, V> loader) {
        if (key == null) {
            return null;
        }
        
        byte[] encryptedValue = delegate.get(key);
        if (encryptedValue == null && loader != null) {
            V value = loader.apply(key);
            if (value != null) {
                put(key, value);
            }
            return value;
        }
        
        if (encryptedValue == null) {
            return null;
        }
        
        try {
            byte[] decryptedValue = encryptor.decrypt(encryptedValue);
            @SuppressWarnings("unchecked")
            V value = (V) SerializationUtils.deserialize(decryptedValue);
            return value;
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
    
    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (key == null) {
            return;
        }
        
        if (value == null) {
            delegate.put(key, null, expireTime, timeUnit);
            return;
        }
        
        try {
            byte[] serializedValue = SerializationUtils.serialize(value);
            byte[] encryptedValue = encryptor.encrypt(serializedValue);
            delegate.put(key, encryptedValue, expireTime, timeUnit);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }
    
    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }
        
        return delegate.remove(key);
    }
    
    @Override
    public void clear() {
        delegate.clear();
    }
} 