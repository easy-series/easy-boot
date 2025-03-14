package com.easy.cache.sync;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.util.Serializer;
import org.springframework.data.redis.core.RedisTemplate;

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

    /**
     * 单例实例
     */
    private static final CacheSyncManager INSTANCE = new CacheSyncManager();

    /**
     * 同步配置映射表
     */
    private final Map<String, SyncConfig> syncConfigMap = new ConcurrentHashMap<>();

    /**
     * 缓存事件发布者
     */
    private CacheEventPublisher publisher;

    /**
     * 缓存事件订阅者
     */
    private CacheEventSubscriber subscriber;

    /**
     * 是否已初始化
     */
    private boolean initialized = false;

    /**
     * 默认同步策略
     */
    private SyncStrategy defaultStrategy = SyncStrategy.INVALIDATE;

    /**
     * 私有构造函数
     */
    private CacheSyncManager() {
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
     * @param redisTemplate Redis模板
     * @param serializer    序列化器
     */
    public synchronized void init(RedisTemplate<String, Object> redisTemplate, Serializer serializer) {
        if (initialized) {
            return;
        }

        // 创建发布者和订阅者
        this.publisher = new RedisPublisher(redisTemplate, serializer);
        this.subscriber = new RedisSubscriber(redisTemplate, serializer);

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
     * 设置默认同步策略
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

        // 如果缓存没有启用同步，则忽略事件
        if (config == null || !config.isSyncEnabled()) {
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
     * @param cacheName   缓存名称
     * @param syncEnabled 是否启用同步
     */
    public void enableSync(String cacheName, boolean syncEnabled) {
        enableSync(cacheName, syncEnabled, defaultStrategy);
    }

    /**
     * 启用缓存同步
     *
     * @param cacheName   缓存名称
     * @param syncEnabled 是否启用同步
     * @param strategy    同步策略
     */
    public void enableSync(String cacheName, boolean syncEnabled, SyncStrategy strategy) {
        if (cacheName == null || cacheName.isEmpty()) {
            return;
        }

        syncConfigMap.put(cacheName, new SyncConfig(syncEnabled, strategy));
        System.out.println("已" + (syncEnabled ? "启用" : "禁用") + "缓存同步: " + cacheName + ", 策略: " + strategy);
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
        private final SyncStrategy syncStrategy;

        public SyncConfig(boolean syncEnabled, SyncStrategy syncStrategy) {
            this.syncEnabled = syncEnabled;
            this.syncStrategy = syncStrategy;
        }

        public boolean isSyncEnabled() {
            return syncEnabled;
        }

        public SyncStrategy getSyncStrategy() {
            return syncStrategy;
        }
    }
}