package com.easy.cache.config;

import com.easy.cache.core.CacheType;
import com.easy.cache.serialization.KeyConvertor;
import com.easy.cache.serialization.ValueDecoder;
import com.easy.cache.serialization.ValueEncoder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存配置类
 * 
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class CacheConfig<K, V> {
    
    /**
     * 缓存名称
     */
    private String name;
    
    /**
     * 过期时间（写入后）
     */
    private long expireAfterWrite;
    
    /**
     * 过期时间单位
     */
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    
    /**
     * 缓存类型
     */
    private CacheType cacheType = CacheType.REMOTE;
    
    /**
     * 本地缓存条目数量限制
     */
    private int localLimit = 100;
    
    /**
     * 是否同步本地缓存（多JVM间）
     */
    private boolean syncLocal = false;
    
    /**
     * 键转换器
     */
    private KeyConvertor keyConvertor;
    
    /**
     * 值编码器
     */
    private ValueEncoder valueEncoder;
    
    /**
     * 值解码器
     */
    private ValueDecoder valueDecoder;
    
    /**
     * 是否缓存null值
     */
    private boolean cacheNullValue = false;
    
    /**
     * 缓存加载器
     */
    private Function<K, V> loader;
    
    /**
     * 刷新策略
     */
    private RefreshPolicy refreshPolicy;
    
    /**
     * 是否启用穿透保护
     */
    private boolean penetrationProtect = false;
    
    /**
     * 创建配置的Builder
     * 
     * @param name 缓存名称
     * @param <K> 键类型
     * @param <V> 值类型
     * @return Builder实例
     */
    public static <K, V> Builder<K, V> newBuilder(String name) {
        return new Builder<>(name);
    }
    
    /**
     * 配置构建器
     * 
     * @param <K> 键类型
     * @param <V> 值类型
     */
    public static class Builder<K, V> {
        private final CacheConfig<K, V> config;
        
        public Builder(String name) {
            config = new CacheConfig<>();
            config.name = name;
        }
        
        public Builder<K, V> expireAfterWrite(long time, TimeUnit unit) {
            config.expireAfterWrite = time;
            config.timeUnit = unit;
            return this;
        }
        
        public Builder<K, V> expireAfterWrite(Duration duration) {
            return expireAfterWrite(duration.toMillis(), TimeUnit.MILLISECONDS);
        }
        
        public Builder<K, V> cacheType(CacheType cacheType) {
            config.cacheType = cacheType;
            return this;
        }
        
        public Builder<K, V> localLimit(int limit) {
            config.localLimit = limit;
            return this;
        }
        
        public Builder<K, V> syncLocal(boolean sync) {
            config.syncLocal = sync;
            return this;
        }
        
        public Builder<K, V> keyConvertor(KeyConvertor keyConvertor) {
            config.keyConvertor = keyConvertor;
            return this;
        }
        
        public Builder<K, V> valueEncoder(ValueEncoder valueEncoder) {
            config.valueEncoder = valueEncoder;
            return this;
        }
        
        public Builder<K, V> valueDecoder(ValueDecoder valueDecoder) {
            config.valueDecoder = valueDecoder;
            return this;
        }
        
        public Builder<K, V> cacheNullValue(boolean cacheNull) {
            config.cacheNullValue = cacheNull;
            return this;
        }
        
        public Builder<K, V> loader(Function<K, V> loader) {
            config.loader = loader;
            return this;
        }
        
        public Builder<K, V> refreshPolicy(RefreshPolicy refreshPolicy) {
            config.refreshPolicy = refreshPolicy;
            return this;
        }
        
        public Builder<K, V> penetrationProtect(boolean protect) {
            config.penetrationProtect = protect;
            return this;
        }
        
        public CacheConfig<K, V> build() {
            return config;
        }
    }
    
    // Getters
    
    public String getName() {
        return name;
    }
    
    public long getExpireAfterWrite() {
        return expireAfterWrite;
    }
    
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
    
    public CacheType getCacheType() {
        return cacheType;
    }
    
    public int getLocalLimit() {
        return localLimit;
    }
    
    public boolean isSyncLocal() {
        return syncLocal;
    }
    
    public KeyConvertor getKeyConvertor() {
        return keyConvertor;
    }
    
    public ValueEncoder getValueEncoder() {
        return valueEncoder;
    }
    
    public ValueDecoder getValueDecoder() {
        return valueDecoder;
    }
    
    public boolean isCacheNullValue() {
        return cacheNullValue;
    }
    
    public Function<K, V> getLoader() {
        return loader;
    }
    
    public RefreshPolicy getRefreshPolicy() {
        return refreshPolicy;
    }
    
    public boolean isPenetrationProtect() {
        return penetrationProtect;
    }
} 