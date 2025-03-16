# Easy-Lock 分布式锁组件

基于 Redis 的高性能分布式锁实现，类似于 Lock4j，提供注解和手动加锁两种使用方式。

## 功能特点

- 支持基于 Redis 的分布式锁实现（可扩展支持 ZooKeeper 和 etcd）
- 支持注解方式声明式加锁
- 支持手动加锁（模板方法）
- 支持锁过期自动释放
- 支持锁获取失败时的重试机制
- 支持锁操作的监控统计
- 支持 Spring EL 表达式构建锁键

## 快速入门

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.easy</groupId>
    <artifactId>easy-lock</artifactId>
    <version>${easy.version}</version>
</dependency>
```

### 2. 配置属性（可选）

在 `application.yml` 中配置：

```yaml
easy:
  lock:
    enabled: true                 # 是否启用分布式锁，默认true
    prefix: easy:lock             # 锁前缀，用于区分不同应用
    expireTime: 30000             # 默认过期时间（毫秒）
    retryCount: 3                 # 默认重试次数
    retryInterval: 100            # 默认重试间隔（毫秒）
    monitorEnabled: true          # 是否启用锁监控
```

### 3. 使用注解加锁

```java
import annotation.com.easy.easy.lock.EasyLock;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    /**
     * 基本用法，锁定用户编辑操作
     */
    @EasyLock(key = "#userId", prefix = "user:edit")
    public void editUser(Long userId, UserDTO userDTO) {
        // 业务逻辑
    }

    /**
     * 高级用法，自定义过期时间和重试策略
     */
    @EasyLock(
            key = "#order.orderNo",
            prefix = "order:pay",
            expire = 10000,
            retryCount = 5,
            retryInterval = 200,
            failStrategy = EasyLock.FailStrategy.IGNORE
    )
    public boolean payOrder(Order order) {
        // 支付处理逻辑
        return true;
    }
}
```

SpEL表达式支持以下变量：
- 方法的所有参数（通过参数名访问）
- `method`：当前方法名
- `class`：当前类名（简单类名）
- `target`：目标对象

### 4. 使用模板手动加锁

```java
import template.com.easy.easy.lock.LockTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class OrderService {

    @Resource
    private LockTemplate lockTemplate;

    public boolean createOrder(Order order) {
        // 加锁并执行业务逻辑
        return lockTemplate.lock("order:create:" + order.getOrderNo(), () -> {
            // 在锁内执行的业务逻辑
            return processOrder(order);
        });
    }

    public boolean cancelOrder(String orderNo) {
        // 自定义锁参数
        return lockTemplate.lock(
                "order:cancel:" + orderNo,  // 锁键
                60000,                      // 过期时间（毫秒）
                3,                          // 重试次数
                200,                        // 重试间隔（毫秒）
                () -> {
                    // 在锁内执行的业务逻辑
                    return processCancelOrder(orderNo);
                }
        );
    }

    public void manualLockOperation(String resourceId) {
        // 获取锁
        LockInfo lockInfo = lockTemplate.tryLock("resource:" + resourceId);

        if (lockInfo != null) {
            try {
                // 获取锁成功，执行业务逻辑
                processResource(resourceId);
            } finally {
                // 释放锁
                lockTemplate.unlock(lockInfo);
            }
        } else {
            // 获取锁失败的处理
            handleLockFailure(resourceId);
        }
    }
}
```

## 扩展开发

### 实现其他锁执行器

如需支持新的锁存储介质（如 ZooKeeper、etcd 等），可以实现 `LockExecutor` 接口：

```java
public class ZookeeperLockExecutor implements LockExecutor {
    
    @Override
    public boolean acquire(String key, String value, long expire) {
        // 实现 ZooKeeper 获取锁逻辑
    }
    
    @Override
    public boolean release(String key, String value) {
        // 实现 ZooKeeper 释放锁逻辑
    }
    
    @Override
    public boolean isLocked(String key) {
        // 实现检查锁状态逻辑
    }
}
```

然后创建对应的锁实现类：

```java
public class ZookeeperLock extends AbstractLock {
    
    private final ZookeeperLockExecutor lockExecutor;
    
    public ZookeeperLock(ZookeeperLockExecutor lockExecutor) {
        this.lockExecutor = lockExecutor;
    }
    
    @Override
    protected LockExecutor getLockExecutor() {
        return lockExecutor;
    }
    
    @Override
    protected LockInfo.LockType getLockType() {
        return LockInfo.LockType.ZOOKEEPER;
    }
}
```

最后，在配置类中注册新实现：

```java
@Bean
@ConditionalOnBean(ZookeeperLockExecutor.class)
@ConditionalOnMissingBean(ZookeeperLock.class)
public ZookeeperLock zookeeperLock(ZookeeperLockExecutor lockExecutor) {
    return new ZookeeperLock(lockExecutor);
}
```

## 锁监控

该组件提供了锁操作的监控指标统计功能：

```java
@Service
public class LockMonitorService {

    @Resource
    private LockMonitor lockMonitor;
    
    public void printLockStatistics() {
        // 获取基本指标
        System.out.println("获取锁成功次数: " + lockMonitor.getSuccessCount());
        System.out.println("获取锁失败次数: " + lockMonitor.getFailCount());
        System.out.println("平均锁定时间: " + lockMonitor.getAverageLockTime() + "ms");
        System.out.println("锁失败比例: " + lockMonitor.getFailRate());
        
        // 获取资源级别统计
        Map<String, LockMonitor.ResourceStats> resourceStats = lockMonitor.getResourceStats();
        for (Map.Entry<String, LockMonitor.ResourceStats> entry : resourceStats.entrySet()) {
            LockMonitor.ResourceStats stats = entry.getValue();
            System.out.println("资源[" + stats.getKey() + "] - 成功次数: " + 
                stats.getSuccessCount() + ", 失败次数: " + stats.getFailCount() + 
                ", 争用比例: " + stats.getContentionRate());
        }
    }
}
```

## 注意事项

1. 分布式锁的键应尽量设计为唯一性强的字符串，避免不必要的锁冲突
2. 合理设置锁过期时间，避免锁长时间不释放导致业务阻塞
3. 在使用 SpEL 表达式时，确保表达式能准确解析目标键值
4. 注意锁的粒度，避免锁粒度过大影响系统并发性能