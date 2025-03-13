package com.easy.cache.sync;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.RedisCache.Serializer;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存同步管理器，负责管理缓存事件的发布和订阅
 */
public class CacheSyncManager {

    /**
     * 缓存同步策略
     */
    public enum SyncStrategy {
        /**
         * 失效模式：收到同步事件后，直接从本地缓存中删除对应的键
         */
        INVALIDATE,

        /**
         * 更新模式：收到同步事件后，直接用事件中的新值更新本地缓存
         */
        UPDATE
    }

    private static final CacheSyncManager INSTANCE = new CacheSyncManager();

    private final Map<String, SyncConfig> syncConfigMap = new ConcurrentHashMap<>();
    private CacheEventPublisher publisher;
    private CacheEventSubscriber subscriber;
    private boolean initialized = false;
    private SyncStrategy defaultStrategy = SyncStrategy.INVALIDATE;

    private CacheSyncManager() {
        // 私有构造函数，防止外部实例化
    }

    /**
     * 获取单例实例
     * 
     * @return 缓存同步管理器实例
     */
    public static CacheSyncManager getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化缓存同步管理器
     * 
     * @param jedisPool  Redis连接池
     * @param serializer 序列化器
     */
    public synchronized void init(JedisPool jedisPool, Serializer serializer) {
        if (initialized) {
            return;
        }

        // 创建发布者和订阅者
        this.publisher = new RedisPublisher(jedisPool, serializer);
        this.subscriber = new RedisSubscriber(jedisPool, serializer);

        // 注册全局事件监听器
        this.subscriber.subscribe(new CacheEventListener() {
            @Override
            public void onEvent(CacheEvent event) {
                handleCacheEvent(event);
            }
        });

        // 启动订阅者
        this.subscriber.start();
        initialized = true;
    }

    /**
     * 设置默认的同步策略
     * 
     * @param strategy 同步策略
     */
    public void setDefaultSyncStrategy(SyncStrategy strategy) {
        this.defaultStrategy = strategy;
    }

    /**
     * 处理缓存事件
     * 
     * @param event 缓存事件
     */
    private void handleCacheEvent(CacheEvent event) {
        if (event == null) {
            return;
        }

        String cacheName = event.getCacheName();
        SyncConfig config = syncConfigMap.get(cacheName);

        // 如果缓存没有启用同步，或者不是本地同步，则忽略事件
        if (config == null || !config.isSyncEnabled() || !config.isSyncLocal()) {
            return;
        }

        // 根据事件类型处理
        switch (event.getEventType()) {
            case PUT:
                handlePutEvent(event, config.getSyncStrategy());
                break;
            case REMOVE:
                handleRemoveEvent(event);
                break;
            case CLEAR:
                handleClearEvent(event);
                break;
            default:
                break;
        }
    }

    /**
     * 处理PUT事件
     * 
     * @param event    缓存事件
     * @param strategy 同步策略
     */
    @SuppressWarnings("unchecked")
    private void handlePutEvent(CacheEvent event, SyncStrategy strategy) {
        String cacheName = event.getCacheName();
        Object key = event.getKey();
        Object value = event.getValue();

        if (key == null) {
            return;
        }

        // 获取本地缓存
        Cache<Object, Object> localCache = getLocalCache(cacheName);
        if (localCache == null) {
            return;
        }

        // 根据同步策略处理
        if (strategy == SyncStrategy.UPDATE && value != null) {
            // 更新模式：直接更新本地缓存
            localCache.put(key, value);
            System.out.println("已同步更新本地缓存: " + cacheName + ", key=" + key);
        } else {
            // 失效模式：从本地缓存中删除，下次访问时会从远程缓存获取
            localCache.remove(key);
            System.out.println("已使本地缓存失效: " + cacheName + ", key=" + key);
        }
    }

    /**
     * 处理REMOVE事件
     * 
     * @param event 缓存事件
     */
    @SuppressWarnings("unchecked")
    private void handleRemoveEvent(CacheEvent event) {
        String cacheName = event.getCacheName();
        Object key = event.getKey();

        if (key == null) {
            return;
        }

        // 获取本地缓存
        Cache<Object, Object> localCache = getLocalCache(cacheName);
        if (localCache == null) {
            return;
        }

        // 从本地缓存中删除
        localCache.remove(key);
        System.out.println("已从本地缓存删除: " + cacheName + ", key=" + key);
    }

    /**
     * 处理CLEAR事件
     * 
     * @param event 缓存事件
     */
    @SuppressWarnings("unchecked")
    private void handleClearEvent(CacheEvent event) {
        String cacheName = event.getCacheName();

        // 获取本地缓存
        Cache<Object, Object> localCache = getLocalCache(cacheName);
        if (localCache == null) {
            return;
        }

        // 清空本地缓存
        localCache.clear();
        System.out.println("已清空本地缓存: " + cacheName);
    }

    /**
     * 获取本地缓存
     * 
     * @param cacheName 缓存名称
     * @return 本地缓存实例
     */
    @SuppressWarnings("unchecked")
    private Cache<Object, Object> getLocalCache(String cacheName) {
        // 从缓存名称中提取本地缓存名称
        String localCacheName = cacheName;
        if (cacheName.contains(":")) {
            // 如果是多级缓存，获取本地缓存部分
            localCacheName = cacheName.split(":")[0] + ":local";
        } else {
            localCacheName = cacheName + ":local";
        }

        try {
            return (Cache<Object, Object>) CacheManager.getInstance().getCache(localCacheName);
        } catch (Exception e) {
            System.err.println("获取本地缓存失败: " + localCacheName + ", " + e.getMessage());
            return null;
        }
    }

    /**
     * 启用缓存同步
     * 
     * @param cacheName 缓存名称
     * @param syncLocal 是否同步本地缓存
     */
    public void enableSync(String cacheName, boolean syncLocal) {
        enableSync(cacheName, syncLocal, defaultStrategy);
    }

    /**
     * 启用缓存同步
     * 
     * @param cacheName 缓存名称
     * @param syncLocal 是否同步本地缓存
     * @param strategy  同步策略
     */
    public void enableSync(String cacheName, boolean syncLocal, SyncStrategy strategy) {
        if (cacheName == null || cacheName.isEmpty()) {
            return;
        }

        syncConfigMap.put(cacheName, new SyncConfig(true, syncLocal, strategy));
        System.out.println("已启用缓存同步: " + cacheName + ", 本地同步: " + syncLocal + ", 策略: " + strategy);
    }

    /**
     * 禁用缓存同步
     * 
     * @param cacheName 缓存名称
     */
    public void disableSync(String cacheName) {
        if (cacheName == null || cacheName.isEmpty()) {
            return;
        }

        SyncConfig config = syncConfigMap.get(cacheName);
        if (config != null) {
            syncConfigMap.put(cacheName, new SyncConfig(false, config.isSyncLocal(), config.getSyncStrategy()));
            System.out.println("已禁用缓存同步: " + cacheName);
        }
    }

    /**
     * 发布缓存事件
     * 
     * @param event 缓存事件
     */
    public void publishEvent(CacheEvent event) {
        if (event == null || !initialized) {
            return;
        }

        String cacheName = event.getCacheName();
        SyncConfig config = syncConfigMap.get(cacheName);

        // 如果缓存没有启用同步，则不发布事件
        if (config == null || !config.isSyncEnabled()) {
            return;
        }

        // 发布事件
        publisher.publish(event);
    }

    /**
     * 关闭缓存同步管理器
     */
    public void shutdown() {
        if (publisher != null) {
            publisher.shutdown();
        }

        if (subscriber != null) {
            subscriber.shutdown();
        }

        initialized = false;
    }

    /**
     * 缓存同步配置
     */
    private static class SyncConfig {
        private final boolean syncEnabled;
        private final boolean syncLocal;
        private final SyncStrategy syncStrategy;

        public SyncConfig(boolean syncEnabled, boolean syncLocal, SyncStrategy syncStrategy) {
            this.syncEnabled = syncEnabled;
            this.syncLocal = syncLocal;
            this.syncStrategy = syncStrategy;
        }

        public boolean isSyncEnabled() {
            return syncEnabled;
        }

        public boolean isSyncLocal() {
            return syncLocal;
        }

        public SyncStrategy getSyncStrategy() {
            return syncStrategy;
        }
    }
}