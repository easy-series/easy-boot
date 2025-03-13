# Easy-Lock 分布式锁框架

基于Redis实现的轻量级分布式锁框架，提供简单易用的注解方式加锁。

## 功能特性

- 支持注解式加锁
- 基于Redis实现，高性能
- 支持锁超时自动释放
- 支持自定义锁名、key表达式
- 支持SpEL表达式解析
- 自动配置，开箱即用

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>easy-lock</artifactId>
    <version>${version}</version>
</dependency>
```

### 2. 配置Redis (如果已配置则忽略)

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

### 3. 使用注解

简单使用：

```java
@Service
public class UserService {
    
    @EasyLock
    public void createUser(User user) {
        // 业务逻辑
    }
}
```

高级配置：

```java
@Service
public class UserService {
    
    @EasyLock(
        name = "user:update", 
        key = "#user.id",
        waitTime = 5000, 
        leaseTime = 60000,
        throwException = true,
        failMessage = "用户信息正在被其他线程修改，请稍后重试"
    )
    public void updateUser(User user) {
        // 业务逻辑
    }
}
```

## 注解参数说明

| 参数           | 类型     | 默认值             | 说明                     |
| -------------- | -------- | ------------------ | ------------------------ |
| name           | String   | 类名.方法名        | 锁的名称                 |
| key            | String   | 方法参数哈希码     | 锁的key，支持SpEL表达式  |
| waitTime       | long     | 3000               | 获取锁的等待时间（毫秒） |
| leaseTime      | long     | 30000              | 锁的持有时间（毫秒）     |
| timeUnit       | TimeUnit | MILLISECONDS       | 时间单位                 |
| throwException | boolean  | true               | 获取锁失败时是否抛出异常 |
| failMessage    | String   | "获取分布式锁失败" | 获取锁失败时的错误消息   |

## 实现原理

Easy-Lock使用Redis实现分布式锁，主要基于以下原理：

1. 使用Lua脚本保证锁操作的原子性
2. 通过设置过期时间，避免死锁
3. 使用唯一标识符作为锁的值，确保只有持有锁的客户端可以释放锁
4. 通过AOP切面拦截带有@EasyLock注解的方法 