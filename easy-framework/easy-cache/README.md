# Easy-Cache 缓存框架

## 一、框架概述

Easy-Cache是一个轻量级、高性能、易扩展的Java缓存抽象框架，提供统一的API和注解驱动功能，支持多级缓存、分布式环境下的缓存一致性等高级特性。框架借鉴了JetCache的优秀设计，并在易用性和扩展性方面进行了增强。

### 核心特性

- **统一的缓存抽象**：提供一致的API接口，屏蔽底层实现差异
- **多级缓存支持**：集成本地缓存（Caffeine）和远程缓存（Redis）的优势
- **注解驱动**：通过简单注解实现方法级缓存管理
- **分布式一致性**：基于事件通知机制实现多节点间缓存同步
- **丰富的扩展点**：支持自定义缓存实现、序列化方式、一致性策略等
- **监控与统计**：提供命中率、QPS等性能指标监控

## 二、架构设计

### 分层架构

```
┌─────────────────────────────────────┐
│           应用层 (Application)       │
├─────────────────────────────────────┤
│           注解层 (Annotation)        │
├─────────────────────────────────────┤
│           抽象层 (Abstraction)       │
├─────────┬─────────────┬─────────────┤
│  本地缓存  │   远程缓存   │  多级缓存    │
│ (Local)  │  (Remote)  │ (MultiLevel)│
├─────────┴─────────────┴─────────────┤
│           基础设施层 (Infrastructure) │
└─────────────────────────────────────┘
```

### 模块划分

1. **easy-cache-core**: 核心接口和抽象类
2. **easy-cache-implementation**: 缓存实现（本地、远程、多级）
3. **easy-cache-annotation**: 注解支持和AOP实现
4. **easy-cache-sync**: 一致性保障与事件机制
5. **easy-cache-serialization**: 序列化支持
6. **easy-cache-monitor**: 监控与统计功能
7. **easy-cache-util**: 工具类集合
8. **easy-cache-spring**: Spring集成支持

## 三、核心组件

### 1. 核心接口

```java
// 缓存基础接口
public interface Cache<K, V> {
    V get(K key);
    void put(K key, V value);
    void put(K key, V value, long expireTime);
    void remove(K key);
    void clear();
    boolean containsKey(K key);
    CacheStats stats();
    String getName();
}

// 多级缓存接口
public interface MultiLevelCache<K, V> extends Cache<K, V> {
    Cache<K, V> getLocalCache();
    Cache<K, V> getRemoteCache();
    ConsistencyPolicy getConsistencyPolicy();
}

// 缓存管理器
public interface CacheManager {
    <K, V> Cache<K, V> getCache(String name);
    <K, V> Cache<K, V> getCache(String name, CacheConfig config);
    void destroyCache(String name);
    Collection<String> getCacheNames();
}
```

### 2. 核心注解

```java
@Cached          // 方法结果缓存
@CacheUpdate     // 更新缓存
@CacheInvalidate // 失效缓存
@CacheRefresh    // 刷新缓存
@EnableCaching   // 启用缓存注解
```

### 3. 多级缓存同步机制

- **写操作顺序**：先写本地缓存，再写远程缓存
- **事件通知**：基于Redis Pub/Sub或消息队列实现
- **过期策略**：本地缓存短过期时间，远程缓存长过期时间
- **分布式锁**：防止缓存击穿

## 四、多级缓存一致性保障

### 1. 写入流程

```
应用程序  →  本地缓存(L1)  →  远程缓存(L2)  →  发布事件  →  其他节点接收事件  →  失效本地缓存
```

### 2. 一致性策略

- **写优先策略**（默认）：先写本地后写远程，保证性能
- **读优先策略**：先写远程后写本地，保证一致性
- **失效策略**：仅在本地写入，远程失效，读取时再加载

### 3. 事件机制

缓存事件（CacheEvent）在以下时机触发：
- 缓存写入/更新时
- 缓存删除时
- 缓存清空时
- 通过注解更新缓存时

## 五、使用示例

### 1. 基础配置

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return CacheBuilder.builder()
                .withLocalCache(new CaffeineCacheBuilder()
                        .maximumSize(10000)
                        .expireAfterWrite(60)
                        .build())
                .withRemoteCache(new RedisCacheBuilder()
                        .redisTemplate(redisTemplate)
                        .expireAfterWrite(300)
                        .build())
                .withConsistencyPolicy(new WriteFirstPolicy())
                .build();
    }
}
```

### 2. 注解使用

```java
@Service
public class UserService {
    @Cached(name = "users", key = "#userId", expire = 3600)
    public User getUser(Long userId) {
        // 查询用户逻辑
        return userRepository.findById(userId).orElse(null);
    }
    
    @CacheUpdate(name = "users", key = "#user.id")
    public void updateUser(User user) {
        // 更新用户逻辑
        userRepository.save(user);
    }
    
    @CacheInvalidate(name = "users", key = "#userId")
    public void deleteUser(Long userId) {
        // 删除用户逻辑
        userRepository.deleteById(userId);
    }
}
```

### 3. 编程式使用

```java
@Service
public class ProductService {
    @Autowired
    private CacheManager cacheManager;
    
    public Product getProduct(Long productId) {
        Cache<Long, Product> cache = cacheManager.getCache("products");
        
        // 尝试从缓存获取
        Product product = cache.get(productId);
        if (product != null) {
            return product;
        }
        
        // 缓存未命中，从数据库加载
        product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            cache.put(productId, product);
        }
        
        return product;
    }
}
```

## 六、性能优化建议

1. **本地缓存配置**
   - 合理设置本地缓存大小和过期时间
   - 选择适当的淘汰策略（LRU/LFU）

2. **远程缓存优化**
   - 使用连接池
   - 启用批量操作
   - 选择高效序列化方式

3. **多级缓存策略**
   - 热点数据优先本地缓存
   - 大容量数据优先远程缓存
   - 差异化过期时间

4. **监控调优**
   - 关注命中率指标
   - 监控慢操作
   - 定期分析使用模式

## 七、扩展机制

框架提供丰富的扩展点，可以根据业务需求进行定制：

1. **缓存实现**：实现Cache接口
2. **序列化方式**：实现Serializer接口
3. **一致性策略**：实现ConsistencyPolicy接口
4. **键生成策略**：实现KeyGenerator接口
5. **监控扩展**：实现CacheMonitor接口

## 八、与JetCache的对比

| 特性         | Easy-Cache   | JetCache     |
| ------------ | ------------ | ------------ |
| 多级缓存     | ✅            | ✅            |
| 注解驱动     | ✅            | ✅            |
| 自动刷新     | ✅            | ✅            |
| 分布式一致性 | 事件驱动     | 事件驱动     |
| 序列化支持   | 多种实现     | 多种实现     |
| 写入顺序     | 先本地后远程 | 先本地后远程 |
| 监控支持     | 多维度       | 基础统计     |
| Spring Boot  | 无缝集成     | 良好支持     |
| 可扩展性     | 丰富扩展点   | 基础扩展     |

## 九、总结

Easy-Cache提供了一套完整的缓存解决方案，通过多级缓存设计、注解驱动和事件机制，在保证性能的同时兼顾了缓存一致性。框架易于使用和扩展，适用于各种规模的Java项目，特别是在分布式环境中表现出色。

无论是简单的方法缓存，还是复杂的多级缓存场景，Easy-Cache都能提供高效、稳定的缓存支持，帮助开发者专注于业务逻辑而非底层缓存实现细节。 