# Easy-ID 分布式ID生成器

基于美团Leaf思想实现的分布式ID生成器，支持雪花算法和号段模式，具有高性能、易扩展的特点。

## 功能特性

- 支持雪花算法（Snowflake）模式
- 支持数据库号段模式（参考美团Leaf-segment）
- 支持Redis号段模式
- 支持业务标识区分不同业务线
- 双Buffer设计，平滑切换号段
- 异步预加载，提升性能
- 自动装配，开箱即用

## 实现原理

### 雪花算法模式

雪花算法（Snowflake）是Twitter开源的分布式ID生成算法，结构如下：

```
0 - 0000000000 0000000000 0000000000 0000000000 0 - 0000000000 - 000000000000
↑   ↑                                            ↑   ↑          ↑
符号位 时间戳（毫秒级）                                机器ID       序列号
```

雪花ID组成部分：
- 1位符号位，始终为0
- 41位时间戳，精确到毫秒
- 10位机器ID，最多支持1024个节点
- 12位序列号，每毫秒内最多生成4096个ID

优点：
- 趋势递增，有利于索引
- 生成速度快，无需网络和存储依赖
- 可解析性强，包含时间和节点信息

缺点：
- 依赖机器时钟，时钟回拨会导致ID重复
- 机器ID需要统一分配和管理

### 号段模式

号段模式是美团Leaf实现的另一种ID生成方案，核心思想是从数据库或Redis预先获取一段号段，然后在内存中分配ID，减少数据库交互。

实现特点：
- 号段预分配：每次向数据库/Redis获取一段ID范围
- 双Buffer设计：当一个Buffer用到一定程度，异步加载下一个Buffer
- 平滑切换：当前Buffer耗尽时，无缝切换到下一个Buffer

优点：
- 减少数据库/Redis访问频率
- 保证ID严格递增
- 支持水平扩展
- 异步加载机制，提高性能

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.iocoder.boot</groupId>
    <artifactId>easy-id</artifactId>
    <version>${version}</version>
</dependency>
```

### 2. 配置选择

在`application.yml`中配置：

```yaml
# 默认配置，使用雪花算法
id-generator:
  type: snowflake
  snowflake:
    worker-id: 1
```

或者使用数据库号段模式：

```yaml
id-generator:
  type: segment-db
  segment:
    step: 1000  # 号段步长
    db:
      table-name: id_generator  # 表名
```

或者使用Redis号段模式：

```yaml
id-generator:
  type: segment-redis
  segment:
    step: 1000  # 号段步长
    redis:
      key-prefix: easy:id  # Redis键前缀
```

### 3. 使用方式

#### 方式一：注入IdGenerator

```java
@Service
public class UserService {

    @Autowired
    private IdGenerator idGenerator;
    
    public void createUser() {
        // 生成ID
        long id = idGenerator.nextId();
        
        // 带业务标识的ID
        long orderId = idGenerator.nextId("order");
    }
}
```

#### 方式二：使用工具类

```java
@Service
public class OrderService {
    
    public void createOrder() {
        // 生成ID
        long id = IdGeneratorUtil.nextId();
        
        // 带业务标识的ID
        long userId = IdGeneratorUtil.nextId("user");
    }
}
```

## 进阶使用

### 数据库表结构

如果使用数据库号段模式，需要创建对应的表结构：

```sql
CREATE TABLE `id_generator` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_key` varchar(128) NOT NULL COMMENT '业务键',
  `current_value` bigint NOT NULL DEFAULT '0' COMMENT '当前值',
  `step` int NOT NULL DEFAULT '1000' COMMENT '步长',
  `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_key` (`business_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分布式ID生成器表';
```

### 解析雪花ID

```java
// 解析时间戳
long timestamp = IdGeneratorUtil.parseTime(id);
Date date = new Date(timestamp);

// 解析序列号
long sequence = IdGeneratorUtil.parseSequence(id);
``` 