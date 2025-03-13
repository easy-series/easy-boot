# Easy Cache 框架

Easy Cache 是一个简单易用的 Java 缓存框架，提供了本地缓存、分布式缓存和多级缓存实现以及注解支持。

## 特性

- 简单易用的 API
- 支持本地缓存
- 支持分布式缓存（Redis）
- 支持多级缓存（本地缓存 + Redis缓存）
- 支持缓存自动刷新
- 支持注解方式使用缓存
- 支持完善的 SpEL 表达式
- 支持缓存过期时间设置
- 支持自定义缓存键生成

## 使用方式

### 1. 直接使用本地缓存 API

```java
// 创建本地缓存
Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
        .name("myCache")
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .buildLocalCache();

// 放入缓存
cache.put("key1", "value1");

// 获取缓存
String value = cache.get("key1");

// 使用加载器获取缓存，如果缓存不存在则加载并缓存
String value2 = cache.get("key2", k -> {
    System.out.println("加载值: " + k);
    return "value for " + k;
});
```

### 2. 使用Redis分布式缓存

首先，需要初始化Redis连接池并配置缓存管理器：

```java
// 初始化Redis连接池
JedisPoolConfig poolConfig = new JedisPoolConfig();
poolConfig.setMaxTotal(10);
poolConfig.setMaxIdle(5);
poolConfig.setMinIdle(1);

JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379);

// 设置缓存管理器
CacheManager cacheManager = CacheManager.getInstance();
cacheManager.setJedisPool(jedisPool);
cacheManager.setSerializer(new JdkSerializer());
```

然后，创建并使用Redis缓存：

```java
// 创建Redis缓存
Cache<String, User> userCache = CacheBuilder.<String, User>newBuilder()
        .name("userCache")
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .useRedis()
        .build();

// 放入缓存
User user = new User("1", "张三");
userCache.put("user:1", user);

// 从缓存获取
User cachedUser = userCache.get("user:1");
```

### 3. 使用多级缓存

#### 3.1 使用二级缓存（本地缓存 + Redis缓存）

```java
// 创建二级缓存
Cache<String, User> userCache = CacheBuilder.<String, User>newBuilder()
        .name("userTwoLevelCache")
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .useTwoLevel()
        .writeThrough(true)  // 写透模式
        .asyncWrite(true)    // 异步写入Redis
        .build();

// 放入缓存
User user = new User("1", "张三");
userCache.put("user:1", user);

// 从缓存获取
User cachedUser = userCache.get("user:1");
```

#### 3.2 使用自定义多级缓存

```java
// 创建各级缓存
Cache<String, User> localCache1 = CacheBuilder.<String, User>newBuilder()
        .name("localCache1")
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .buildLocalCache();

Cache<String, User> localCache2 = CacheBuilder.<String, User>newBuilder()
        .name("localCache2")
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .buildLocalCache();

Cache<String, User> redisCache = CacheBuilder.<String, User>newBuilder()
        .name("redisCache")
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .buildRedisCache();

// 创建多级缓存
Cache<String, User> userCache = CacheBuilder.<String, User>newBuilder()
        .name("userMultiLevelCache")
        .useMultiLevel()
        .addCache(localCache1)  // 一级缓存（最高优先级）
        .addCache(localCache2)  // 二级缓存
        .addCache(redisCache)   // 三级缓存（最低优先级）
        .writeThrough(true)
        .asyncWrite(true)
        .build();
```

多级缓存的特点：
- 读取时按照优先级从高到低查询，找到即返回
- 如果在低优先级缓存中找到值，会自动回填到高优先级缓存中
- 写入时可以选择写透模式（同时写入所有缓存层）或非写透模式（只写入最高优先级缓存）
- 可以选择同步或异步写入低优先级缓存

### 4. 使用缓存自动刷新

缓存自动刷新功能允许缓存在后台定期更新，避免缓存过期导致的性能问题。

#### 4.1 创建可自动刷新的缓存

```java
// 创建可自动刷新的本地缓存，每5分钟刷新一次
Cache<String, String> timeCache = CacheBuilder.<String, String>newBuilder()
        .name("timeCache")
        .refreshable()                      // 启用自动刷新
        .refreshInterval(5, TimeUnit.MINUTES) // 设置刷新间隔
        .refreshThreadPoolSize(2)           // 设置刷新线程池大小
        .build();

// 使用加载器获取缓存，同时注册自动刷新
String time = timeCache.get("current_time", k -> {
    return new Date().toString();  // 这个加载器会被自动注册用于刷新
});
```

#### 4.2 手动注册刷新器

```java
// 如果需要手动注册刷新器
if (cache instanceof RefreshableCache) {
    RefreshableCache<String, String> refreshableCache = (RefreshableCache<String, String>) cache;
    refreshableCache.registerLoader("key", k -> {
        // 返回新的值
        return "new value for " + k;
    });
}
```

#### 4.3 创建可自动刷新的多级缓存

```java
// 创建可自动刷新的二级缓存
Cache<String, User> userCache = CacheBuilder.<String, User>newBuilder()
        .name("userTwoLevelCache")
        .useTwoLevel()
        .writeThrough(true)
        .asyncWrite(true)
        .refreshable()
        .refreshInterval(10, TimeUnit.MINUTES)
        .build();
```

缓存自动刷新的特点：
- 在后台定期刷新缓存，避免缓存过期导致的性能问题
- 刷新失败不会影响缓存的正常使用
- 支持所有类型的缓存（本地缓存、Redis缓存、多级缓存）
- 可以手动注册刷新器，也可以通过加载器自动注册
- 如果缓存项被移除，对应的刷新任务也会被自动取消

### 5. 使用注解和 SpEL 表达式

在方法上添加 `@Cached` 注解，并使用 SpEL 表达式定义缓存键：

```java
// 基本用法
@Cached(expire = 5, timeUnit = TimeUnit.MINUTES)
public User getUserById(String id) {
    System.out.println("从数据库加载用户: " + id);
    return new User(id, "用户" + id);
}

// 使用 SpEL 表达式
@Cached(
    key = "user:#{#p0}:#{#p1}", 
    expire = 5, 
    timeUnit = TimeUnit.MINUTES,
    cacheType = CacheType.TWO_LEVEL,
    refresh = true,
    refreshInterval = 30,
    refreshTimeUnit = TimeUnit.SECONDS
)
public User getUserByIdAndType(String id, String type) {
    System.out.println("从数据库加载用户: " + id + ", 类型: " + type);
    return new User(id, "用户" + id + "(" + type + ")");
}
```

#### 5.1 SpEL 表达式语法

SpEL 表达式使用 `#{...}` 语法，支持以下特性：

- 参数引用：
  - `#{#p0}`：第一个参数
  - `#{#p1}`：第二个参数
  - `#{#args[0]}`：第一个参数（数组形式）

- 属性访问：
  - `#{#p0.id}`：第一个参数的 id 属性
  - `#{#user.name}`：名为 user 的参数的 name 属性

- 方法信息：
  - `#{methodName}`：方法名
  - `#{#targetClass.simpleName}`：目标类的简单名称

- 复合表达式：
  - `user:#{#p0}:#{#p1}`：组合多个表达式

#### 5.2 自定义 SpEL 键生成器

```java
// 创建自定义 SpEL 键生成器
SpelKeyGenerator keyGenerator = new SpelKeyGenerator("order:#{#p0}:#{#p1}");

// 创建缓存拦截器
CacheInterceptor interceptor = new CacheInterceptor(keyGenerator, cacheManager);
```

注解参数说明：

- `name`: 缓存名称，默认为类名.方法名
- `key`: 缓存键，支持 SpEL 表达式，默认使用方法参数
- `expire`: 过期时间，默认为0表示永不过期
- `timeUnit`: 过期时间单位，默认为秒
- `cacheNull`: 是否缓存null值，默认为false
- `cacheType`: 缓存类型，可选 LOCAL、REDIS、TWO_LEVEL
- `refresh`: 是否自动刷新缓存，默认为false
- `refreshInterval`: 刷新间隔，默认为60
- `refreshTimeUnit`: 刷新间隔时间单位，默认为秒

## 核心组件

- `Cache`: 缓存接口，定义基本的缓存操作
- `LocalCache`: 基于内存的本地缓存实现
- `RedisCache`: 基于Redis的分布式缓存实现
- `MultiLevelCache`: 多级缓存实现，支持组合多个缓存实例
- `RefreshableCache`: 可自动刷新的缓存装饰器
- `CacheManager`: 缓存管理器，用于创建和管理缓存实例
- `CacheBuilder`: 缓存构建器，用于创建缓存实例
- `Cached`: 缓存注解，用于标记需要缓存结果的方法
- `CacheInterceptor`: 缓存注解处理器，用于处理缓存注解
- `SpelExpressionParser`: SpEL表达式解析器
- `SpelKeyGenerator`: 基于SpEL的缓存键生成器
- `Serializer`: 序列化器接口，用于Redis缓存的对象序列化
- `JdkSerializer`: 基于JDK的序列化器实现

## 示例

- 本地缓存示例请参考 `CacheExample` 类
- Redis缓存示例请参考 `RedisCacheExample` 类
- 多级缓存示例请参考 `MultiLevelCacheExample` 类
- 可自动刷新缓存示例请参考 `RefreshableCacheExample` 类
- SpEL表达式示例请参考 `SpelCacheExample` 类
- Spring Boot集成示例请参考 `SpringBootCacheExample` 类

## Spring Boot 集成

Easy Cache 框架提供了与 Spring Boot 的无缝集成，只需添加相关配置即可使用。

### 1. 配置

在 `application.yml` 或 `application.properties` 中添加以下配置：

```yaml
# Easy Cache 配置
easy:
  cache:
    enabled: true
    local:
      enabled: true
      maximum-size: 10000
      initial-capacity: 100
      expire-after-write: 30
      time-unit: MINUTES
    redis:
      enabled: true
      host: localhost
      port: 6379
      password: 
      database: 0
      timeout: 2000
      max-total: 8
      max-idle: 8
      min-idle: 0
      expire-after-write: 1
      time-unit: HOURS
      serializer: JSON
    multi-level:
      enabled: true
      write-through: true
      async-write: true
```

### 2. 使用注解

在 Spring 组件中使用 `@Cached` 注解：

```java
@Service
public class UserService {

    /**
     * 使用本地缓存
     */
    @Cached(key = "user:#{#p0}", expire = 5, timeUnit = TimeUnit.MINUTES)
    public User getUserById(String id) {
        System.out.println("从数据库加载用户: " + id);
        return new User(id, "用户" + id);
    }

    /**
     * 使用二级缓存（本地+Redis）
     */
    @Cached(
        key = "user:#{#p0}:#{#p1}", 
        expire = 5, 
        timeUnit = TimeUnit.MINUTES,
        cacheType = CacheType.TWO_LEVEL,
        refresh = true,
        refreshInterval = 30,
        refreshTimeUnit = TimeUnit.SECONDS
    )
    public User getUserByIdAndType(String id, String type) {
        System.out.println("从数据库加载用户: " + id + ", 类型: " + type);
        return new User(id, "用户" + id + "(" + type + ")");
    }
}
```

### 3. 手动注入缓存管理器

如果需要手动创建和管理缓存，可以注入 `CacheManager`：

```java
@Service
public class CacheService {

    private final CacheManager cacheManager;
    private final Cache<String, User> userCache;

    @Autowired
    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        
        // 创建缓存
        this.userCache = CacheBuilder.<String, User>newBuilder()
                .name("userCache")
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .useTwoLevel()
                .build();
    }

    public User getUser(String id) {
        return userCache.get(id, k -> {
            System.out.println("加载用户: " + k);
            return new User(k, "用户" + k);
        });
    }
}
```

### 4. 配置属性说明

| 属性                                   | 说明                          | 默认值      |
| -------------------------------------- | ----------------------------- | ----------- |
| `easy.cache.enabled`                   | 是否启用缓存                  | `true`      |
| `easy.cache.local.enabled`             | 是否启用本地缓存              | `true`      |
| `easy.cache.local.maximum-size`        | 本地缓存最大项数              | `10000`     |
| `easy.cache.local.initial-capacity`    | 本地缓存初始容量              | `100`       |
| `easy.cache.local.expire-after-write`  | 本地缓存默认过期时间          | `30`        |
| `easy.cache.local.time-unit`           | 本地缓存过期时间单位          | `MINUTES`   |
| `easy.cache.redis.enabled`             | 是否启用Redis缓存             | `false`     |
| `easy.cache.redis.host`                | Redis主机                     | `localhost` |
| `easy.cache.redis.port`                | Redis端口                     | `6379`      |
| `easy.cache.redis.password`            | Redis密码                     | `null`      |
| `easy.cache.redis.database`            | Redis数据库索引               | `0`         |
| `easy.cache.redis.timeout`             | Redis连接超时时间（毫秒）     | `2000`      |
| `easy.cache.redis.max-total`           | Redis连接池最大连接数         | `8`         |
| `easy.cache.redis.max-idle`            | Redis连接池最大空闲连接数     | `8`         |
| `easy.cache.redis.min-idle`            | Redis连接池最小空闲连接数     | `0`         |
| `easy.cache.redis.expire-after-write`  | Redis缓存默认过期时间         | `1`         |
| `easy.cache.redis.time-unit`           | Redis缓存过期时间单位         | `HOURS`     |
| `easy.cache.redis.serializer`          | Redis序列化器类型（JDK/JSON） | `JSON`      |
| `easy.cache.multi-level.enabled`       | 是否启用多级缓存              | `false`     |
| `easy.cache.multi-level.write-through` | 是否使用写透模式              | `true`      |
| `easy.cache.multi-level.async-write`   | 是否异步写入低级缓存          | `true`      |

## 扩展

框架设计支持扩展，可以通过以下方式进行扩展：

1. 实现 `Cache` 接口，创建自定义缓存实现
2. 实现 `KeyGenerator` 接口，创建自定义缓存键生成器
3. 实现 `Serializer` 接口，创建自定义序列化器

## 注意事项

- 使用Redis缓存时，需要确保缓存的对象实现了 `Serializable` 接口
- 在生产环境中使用时，建议根据实际需求进行扩展和定制
- 使用Redis缓存时，需要添加Jedis依赖：

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>3.7.0</version>
</dependency>
```

## QuickConfig 快速配置

Easy Cache 提供了 `QuickConfig` 类，用于快速配置和创建缓存实例，类似于 JetCache 的 `QuickConfig`。

### 基本用法

```java
// 创建缓存管理器
CacheManager cacheManager = CacheManager.getInstance();

// 创建一个本地缓存配置
QuickConfig localConfig = QuickConfig.builder()
        .name("userCache")
        .expire(30, TimeUnit.MINUTES)
        .cacheType(QuickConfig.CacheType.LOCAL)
        .build();

// 获取或创建缓存
Cache<String, User> userCache = cacheManager.getOrCreateCache(localConfig);

// 使用缓存
userCache.put("user:1", new User("1", "张三", 25));
User user = userCache.get("user:1");
```

### 配置选项

`QuickConfig` 支持以下配置选项：

| 配置项          | 说明                               | 默认值           |
| --------------- | ---------------------------------- | ---------------- |
| name            | 缓存名称                           | 必填             |
| expire          | 过期时间                           | 0（永不过期）    |
| timeUnit        | 过期时间单位                       | TimeUnit.SECONDS |
| cacheType       | 缓存类型（LOCAL/REMOTE/BOTH）      | LOCAL            |
| cacheNull       | 是否缓存空值                       | false            |
| refreshable     | 是否启用自动刷新                   | false            |
| refreshInterval | 刷新间隔                           | 1                |
| refreshTimeUnit | 刷新间隔时间单位                   | TimeUnit.MINUTES |
| writeThrough    | 是否启用写透模式（仅两级缓存有效） | false            |
| asyncWrite      | 是否启用异步写入（仅两级缓存有效） | false            |

### 缓存类型

`QuickConfig.CacheType` 枚举定义了三种缓存类型：

- `LOCAL`：本地缓存，使用内存存储
- `REMOTE`：远程缓存，使用 Redis 存储
- `BOTH`：两级缓存，同时使用本地缓存和 Redis 缓存

### 自动刷新

通过设置 `refreshable` 为 true，可以启用缓存自动刷新功能：

```java
QuickConfig refreshableConfig = QuickConfig.builder()
        .name("stockCache")
        .expire(5, TimeUnit.MINUTES)
        .cacheType(QuickConfig.CacheType.LOCAL)
        .refreshable(true)
        .refreshInterval(1, TimeUnit.MINUTES)
        .build();

Cache<String, Integer> stockCache = cacheManager.getOrCreateCache(refreshableConfig);

// 注册刷新函数
stockCache.registerLoader("stock:P001", key -> {
    // 从数据源获取最新数据
    return 95;
});
```

### 在 Spring 中使用

在 Spring 应用中，可以在 `@Component` 类中使用 `@PostConstruct` 初始化缓存：

```java
@Component
public class CacheService {
    
    @Autowired
    private CacheManager cacheManager;
    
    private Cache<String, User> userCache;
    private Cache<String, Order> orderCache;
    
    @PostConstruct
    public void init() {
        // 初始化用户缓存
        userCache = cacheManager.getOrCreateCache(
            QuickConfig.builder()
                .name("userCache")
                .expire(30, TimeUnit.MINUTES)
                .cacheType(QuickConfig.CacheType.LOCAL)
                .build()
        );
        
        // 初始化订单缓存
        orderCache = cacheManager.getOrCreateCache(
            QuickConfig.builder()
                .name("orderCache")
                .expire(1, TimeUnit.HOURS)
                .cacheType(QuickConfig.CacheType.BOTH)
                .writeThrough(true)
                .build()
        );
    }
    
    // 使用缓存的方法...
}
``` 