# Easy Cache 缓存框架

## 项目简介

Easy Cache 是一个灵活高效的 Java 缓存框架，专为 Java/Spring 应用设计。它提供了多级缓存、本地缓存同步、缓存统计等功能，满足各种缓存场景需求。主要特点包括：

- **多级缓存**：支持本地缓存（Caffeine）和远程缓存（Redis）的组合使用
- **缓存同步**：基于 Redis 的本地缓存自动同步机制
- **防止缓存穿透**：内置缓存穿透保护机制
- **统计监控**：详细的缓存命中率和性能统计
- **简单易用**：流畅的 API 设计，与 Spring 无缝集成
- **可扩展性**：灵活的扩展点设计，支持自定义缓存实现

## 核心组件

### 接口层

- **Cache**：核心缓存接口，定义基本的缓存操作
- **LocalCache**：本地缓存接口，提供本地缓存特有的功能
- **CacheManager**：缓存管理器，负责缓存的创建和管理
- **CacheTemplate**：缓存操作模板，提供简便的缓存调用方式

### 实现类

- **CaffeineCache**：基于 Caffeine 的本地缓存实现
- **RedisCache**：基于 Redis 的远程缓存实现
- **MultiLevelCache**：多级缓存实现，组合本地缓存和远程缓存
- **DefaultCacheManager**：默认缓存管理器实现

### 同步机制

- **CacheEvent**：缓存事件，描述缓存变更
- **CacheEventPublisher**：缓存事件发布器
- **LocalCacheSyncManager**：本地缓存同步管理器

### 配置类

- **CacheConfig**：详细的缓存配置
- **QuickConfig**：快速缓存配置创建器
- **EasyCacheProperties**：框架属性配置类

## 特性详解

### 多级缓存

支持三种缓存类型：

- **本地缓存**：基于 Caffeine，适合高频访问的小体积数据
- **远程缓存**：基于 Redis，适合分布式环境下的共享数据
- **多级缓存**：组合两者优势，先查本地，再查远程，实现高性能和一致性的平衡

### 本地缓存同步

在分布式环境中，通过 Redis 发布/订阅机制，自动同步不同节点的本地缓存变更：

- 一个节点更新缓存时，发布缓存变更事件
- 其他节点接收事件，更新本地缓存
- 支持三种事件类型：PUT、REMOVE、CLEAR

### 缓存统计

内置缓存统计功能：

- 请求次数统计
- 命中次数统计
- 命中率计算
- 缓存大小监控

### 防止缓存穿透

提供多种机制防止缓存穿透：

- 缓存空值功能
- 并发请求合并执行
- 布隆过滤器支持（可扩展）

## 使用方法

### 基础配置

在 Spring Boot 应用中引入依赖：

```xml
<dependency>
    <groupId>com.easy</groupId>
    <artifactId>easy-cache</artifactId>
    <version>${version}</version>
</dependency>
```

application.yml 配置：

```yaml
easy:
  cache:
    enabled: true
    sync-channel: "easy:cache:sync"
    default-expire: 3600
    default-local-limit: 10000
    default-sync-local: true
    default-cache-null-values: true
    default-penetration-protect: false
```

### 使用 CacheTemplate

在服务中注入 CacheTemplate：

```java
@Service
public class UserService {

    private final UserRepository userRepository;
    private final CacheTemplate cacheTemplate;
    private final Cache<Long, User> userCache;

    public UserService(UserRepository userRepository, CacheTemplate cacheTemplate) {
        this.userRepository = userRepository;
        this.cacheTemplate = cacheTemplate;
        
        // 创建用户缓存
        this.userCache = cacheTemplate.createCache(
            QuickConfig.newBuilder("userCache")
                .cacheType(CacheType.BOTH)  // 使用多级缓存
                .expire(Duration.ofMinutes(30))  // 设置过期时间
                .localLimit(1000)  // 本地缓存最大元素数
                .syncLocal(true)  // 启用本地缓存同步
                .build()
        );
    }

    public User getUser(Long id) {
        // 使用已创建的缓存
        return userCache.computeIfAbsent(id, userId -> userRepository.findById(userId));
        
        // 或者直接使用 CacheTemplate
        // return cacheTemplate.computeIfAbsent("userCache", id, 
        //     userId -> userRepository.findById(userId));
    }

    public void updateUser(User user) {
        userRepository.save(user);
        userCache.put(user.getId(), user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        userCache.remove(id);
    }
}
```

### 高级配置

使用 CacheConfig 创建自定义缓存：

```java
Cache<String, Order> orderCache = cacheTemplate.createCache(
    CacheConfig.<String, Order>builder()
        .name("orderCache")
        .cacheType(CacheType.BOTH)
        .expireAfterWrite(Duration.ofHours(1))
        .localLimit(500)
        .syncLocal(true)
        .cacheNullValues(true)
        .penetrationProtect(true)
        .writeThrough(true)
        .writer((key, value) -> {
            // 回写到数据库或其他持久化存储
            orderRepository.updateOrderStatus(value);
        })
        .loader(key -> {
            // 加载器函数，当缓存不存在时调用
            return orderRepository.findByOrderNumber(key);
        })
        .build()
);
```

## 扩展点

### 自定义 KeyConvertor

实现 KeyConvertor 接口，自定义缓存键转换逻辑：

```java
public class MyKeyConvertor implements KeyConvertor {
    @Override
    public String convert(Object key) {
        // 自定义键转换逻辑
        if (key instanceof User) {
            return "user:" + ((User) key).getId();
        }
        return key.toString();
    }
}
```

### 自定义缓存实现

实现 Cache 接口或继承 AbstractCache，创建自定义缓存实现：

```java
public class MyCustomCache<K, V> extends AbstractCache<K, V> {
    // 实现缓存逻辑
}
```

## 最佳实践

1. **合理选择缓存类型**：
   - 高频访问、变动少的数据：多级缓存
   - 频繁变更的数据：远程缓存
   - 临时计算结果：本地缓存

2. **设置合理的过期时间**：根据数据更新频率和重要性设置

3. **合理使用缓存空值**：避免缓存穿透，但需控制空值数量

4. **注意缓存同步**：在分布式环境中开启本地缓存同步

5. **监控缓存状态**：定期检查缓存命中率，及时调整策略

## 性能考量

- **本地缓存**：纳秒级访问速度，几乎零延迟
- **远程缓存**：毫秒级访问速度，有网络开销
- **多级缓存**：绝大多数情况下纳秒级响应，少数情况毫秒级

## 示例项目

参考测试类 `CacheTest.java` 了解详细用法示例。

## 贡献指南

欢迎提交 Issue 和 Pull Request 贡献代码。

## 许可证

[Apache License 2.0](LICENSE)