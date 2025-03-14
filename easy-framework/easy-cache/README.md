# Easy Cache 缓存框架

Easy Cache 是一个灵活、高性能的Java缓存框架，提供了多级缓存、热点缓存保护、缓存穿透保护等多种实用功能。该框架基于Spring Boot开发，可以轻松集成到Spring应用中。

## 主要特性

- 多级缓存：支持本地缓存、Redis缓存、两级缓存等多种缓存类型
- 注解驱动：通过简单的注解实现缓存操作，无需编写重复代码
- 缓存同步：支持多实例间的缓存同步
- 自动刷新：支持缓存定时自动刷新
- 热点保护：防止缓存击穿，保护热点数据
- 穿透保护：防止缓存穿透，保护系统安全
- 灵活扩展：支持自定义序列化、自定义键生成器等
- 数据安全：支持加密缓存，保护敏感数据
- Spring Boot集成：提供自动配置，开箱即用

## 核心组件

### 注解

| 注解名称 | 说明 |
| ------- | ---- |
| `@EnableCaching` | 启用缓存功能，通常标注在配置类上 |
| `@Cached` | 标记需要缓存的方法 |
| `@CacheUpdate` | 标记需要更新缓存的方法 |
| `@CacheInvalidate` | 标记需要使缓存失效的方法 |
| `@CacheRefresh` | 配置缓存自动刷新 |
| `@CachePenetrationProtect` | 启用缓存穿透保护 |
| `@HotKeyProtect` | 启用热点键保护 |

### 核心类

#### 缓存接口和实现

| 类名称 | 说明 |
| ----- | ---- |
| `Cache` | 缓存接口，定义缓存的基本操作 |
| `AbstractCache` | 缓存接口的抽象实现类 |
| `LocalCache` | 本地缓存实现，基于Guava Cache |
| `RedisCache` | Redis缓存实现 |
| `MultiLevelCache` | 多级缓存实现，结合本地缓存和远程缓存 |
| `HotKeyCache` | 热点缓存实现，用于防止缓存击穿 |
| `BloomFilterCache` | 布隆过滤器缓存实现，用于防止缓存穿透 |
| `CircuitBreakerCache` | 断路器缓存实现，用于处理依赖故障 |
| `EncryptedCache` | 加密缓存实现，用于敏感数据 |
| `RefreshableCache` | 可刷新缓存实现，支持自动刷新 |

#### 缓存管理和构建

| 类名称 | 说明 |
| ----- | ---- |
| `CacheManager` | 缓存管理器，用于创建和管理缓存实例 |
| `CacheBuilder` | 缓存构建器，用于构建不同类型的缓存 |
| `CacheType` | 缓存类型枚举，包括LOCAL、REDIS、TWO_LEVEL |
| `CacheConfig` | 缓存配置类，用于配置缓存参数 |
| `QuickConfig` | 快速配置类，提供便捷的配置方法 |

#### 缓存同步机制

| 类名称 | 说明 |
| ----- | ---- |
| `CacheEvent` | 缓存事件类，表示缓存的变更操作 |
| `CacheEventPublisher` | 缓存事件发布器接口 |
| `CacheEventSubscriber` | 缓存事件订阅器接口 |
| `CacheEventListener` | 缓存事件监听器接口 |
| `CacheSyncManager` | 缓存同步管理器，负责缓存同步策略 |
| `RedisPublisher` | 基于Redis的事件发布器实现 |
| `RedisSubscriber` | 基于Redis的事件订阅器实现 |

#### 安全机制

| 类名称 | 说明 |
| ----- | ---- |
| `Encryptor` | 缓存加密接口，用于加密/解密缓存数据 |
| `AesEncryptor` | AES加密实现，提供对称加密功能 |

#### Spring Boot集成

| 类名称 | 说明 |
| ----- | ---- |
| `EasyCacheAutoConfiguration` | Spring Boot自动配置类，提供自动配置 |
| `CacheAutoConfiguration` | 备用自动配置类，兼容旧版本 |
| `EasyCacheAspect` | Spring AOP切面实现，用于处理注解 |
| `CacheProperties` | 配置属性类，支持通过application.yml配置 |

#### AOP实现

| 类名称 | 说明 |
| ----- | ---- |
| `CacheAspect` | 缓存切面，用于拦截缓存注解 |
| `CacheInterceptor` | 缓存拦截器，用于处理缓存操作 |

#### 支持类

| 类名称 | 说明 |
| ----- | ---- |
| `KeyGenerator` | 缓存键生成器接口 |
| `DefaultKeyGenerator` | 默认缓存键生成器实现 |
| `SpelKeyGenerator` | 基于SpEL表达式的键生成器 |
| `SpELParser` | SpEL表达式解析器，用于解析缓存键表达式 |
| `JdkSerializer` | JDK序列化器实现 |
| `FastJsonSerializer` | 基于FastJson的序列化器实现 |

## 使用示例

### 1. 启用缓存

```java
@EnableCaching(
    enableLocalCache = true,
    enableRemoteCache = true,
    enableCacheSync = true,
    enableAutoRefresh = true,
    enablePenetrationProtect = true
)
@Configuration
public class AppConfig {
    // 配置代码
}
```

### 2. 使用缓存

```java
@Service
public class UserService {
    
    @Cached(
        name = "user",
        key = "#userId",
        expire = 3600,
        cacheType = CacheType.TWO_LEVEL,
        cacheNull = true
    )
    @CacheRefresh(refresh = 1800)
    @CachePenetrationProtect
    public User getUserById(Long userId) {
        // 从数据库查询用户
        return userDao.findById(userId);
    }
    
    @CacheUpdate(name = "user", key = "#user.id", value = "#user")
    public User updateUser(User user) {
        // 更新用户
        return userDao.update(user);
    }
    
    @CacheInvalidate(name = "user", key = "#userId")
    public void deleteUser(Long userId) {
        // 删除用户
        userDao.deleteById(userId);
    }
}
```

## 配置参数

### application.yml 配置示例

```yaml
easy:
  cache:
    enabled: true
    # 本地缓存配置
    local:
      enabled: true
      initial-capacity: 100
      maximum-size: 10000
      expire-after-write: 30
      time-unit: MINUTES
    # Redis缓存配置
    redis:
      enabled: true
      host: localhost
      port: 6379
      password: 123456
      database: 0
      timeout: 2000
      max-total: 8
      max-idle: 8
      min-idle: 0
      serializer: JSON
      expire-after-write: 1
      time-unit: HOURS
    # 多级缓存配置
    multi-level:
      enabled: true
      write-through: true
      async-write: false
```

## 缓存同步机制

Easy Cache 支持以下几种缓存同步机制：

1. **基于Redis发布订阅**：使用Redis的pub/sub机制实现多实例间的缓存同步
2. **基于消息队列**：使用MQ实现多实例间的缓存同步，支持更复杂的场景
3. **定时刷新**：通过定时任务刷新缓存，适用于对实时性要求不高的场景

启用缓存同步只需要在`@EnableCaching`注解中设置`enableCacheSync = true`，然后在配置文件中指定同步类型即可。

## 加密缓存

Easy Cache 支持对敏感数据进行加密存储，使用方式如下：

```java
// 创建加密器
Encryptor encryptor = new AesEncryptor("your-secret-key");

// 创建加密缓存
Cache<String, User> userCache = CacheBuilder.newBuilder()
    .name("encryptedUserCache")
    .encryptor(encryptor)
    .build();
```

## Spring Boot Starter

Easy Cache 提供了Spring Boot Starter，可以通过以下方式引入：

```xml
<dependency>
    <groupId>com.easy</groupId>
    <artifactId>easy-cache-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>
```

引入后，只需在配置类上添加`@EnableCaching`注解即可启用缓存功能。配置参数可以在`application.yml`中设置。

## 注意事项

- 缓存键应确保唯一性，避免缓存冲突
- 合理设置缓存过期时间，避免内存溢出
- 对于频繁变更的数据，应谨慎使用缓存或设置较短的过期时间
- 使用缓存穿透保护时应注意布隆过滤器的误判率
- 启用缓存同步机制会带来一定的性能开销，应根据实际需求选择是否启用
- 敏感数据应使用加密缓存存储，防止数据泄露