# Easy-ID 分布式ID生成器

> 基于Leaf实现的分布式ID生成系统，提供雪花算法和号段模式两种实现，同时扩展了Redis号段模式。

## 功能特性

1. **多种ID生成策略**
   - 雪花算法（Snowflake）：高性能、趋势递增、全局唯一
   - 号段模式（Segment）：基于数据库，ID连续性好
   - Redis号段模式：基于Redis实现的号段模式，性能更佳

2. **简单易用**
   - Spring Boot Starter方式接入，开箱即用
   - 提供统一的IdTemplate接口，类似RestTemplate，快速获取ID

3. **高性能与可用性**
   - 基于双Buffer设计，提高号段模式性能
   - 支持异步加载下一个号段，减少阻塞
   - 雪花算法支持时钟回拨处理

4. **监控与统计**
   - 自动收集ID生成的性能指标
   - 提供请求量、响应时间等监控数据

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.easy</groupId>
    <artifactId>easy-id</artifactId>
    <version>${version}</version>
</dependency>
```

### 2. 配置属性

```yaml
easy:
  id:
    # 默认的ID生成器类型: snowflake, segment, redis-segment
    default-type: snowflake
    
    # 雪花算法配置
    snowflake:
      enabled: true
      worker-id: 1
      data-center-id: 1
      name: snowflake
    
    # 号段模式配置 (基于数据库)
    segment:
      enabled: false
      table-name: easy_id_allocator
      name: segment
      # 号段模式的业务配置
      biz-configs:
        user:
          biz-key: user_id
          step: 1000
          description: 用户ID
        order:
          biz-key: order_id
          step: 2000
          description: 订单ID
    
    # Redis号段模式配置
    redis-segment:
      enabled: false
      key-prefix: easy:id:segment:
      name: redis-segment
      # Redis号段模式的业务配置
      biz-configs:
        product:
          biz-key: product_id
          step: 500
          description: 商品ID
        payment:
          biz-key: payment_id
          step: 1000
          description: 支付ID
```

### 3. 使用方式

```java
@RestController
@RequestMapping("/api/id")
public class IdController {

    @Autowired
    private IdTemplate idTemplate;
    
    @GetMapping("/next")
    public Long nextId() {
        // 使用默认生成器获取ID
        return idTemplate.nextId();
    }
    
    @GetMapping("/batch")
    public long[] batchIds(@RequestParam(defaultValue = "10") int count) {
        // 批量获取ID
        return idTemplate.nextId(count);
    }
    
    @GetMapping("/snowflake")
    public Long snowflakeId() {
        // 使用指定名称的生成器获取ID
        return idTemplate.nextId("snowflake");
    }
    
    @GetMapping("/segment/user")
    public Long userIdWithSegment() {
        // 使用指定业务键获取ID
        SegmentIdGenerator generator = (SegmentIdGenerator) idTemplate.getGenerators().get("segment");
        return generator.nextId("user_id");
    }
    
    @GetMapping("/redis/product")
    public Long productIdWithRedis() {
        // 使用Redis号段生成器获取ID
        SegmentIdGenerator generator = (SegmentIdGenerator) idTemplate.getGenerators().get("redis-segment");
        return generator.nextId("product_id");
    }
}
```

### 4. 测试方法

Easy-ID提供了多种测试方法，帮助开发者快速验证功能：

1. **单元测试**  
   使用`IdGeneratorTest`进行单元测试，验证雪花算法性能和正确性：
   ```java
   // 执行单元测试
   mvn test -Dtest=IdGeneratorTest
   ```

2. **Spring Boot集成测试**  
   使用`EasyIdSpringBootTest`测试在Spring Boot环境中的集成：
   ```java
   // 执行集成测试
   mvn test -Dtest=EasyIdSpringBootTest
   ```

3. **手动测试**  
   提供`ManualTest`类用于快速本地测试：
   ```java
   // 编译并运行
   mvn compile exec:java -Dexec.mainClass="com.easy.id.test.ManualTest"
   ```

## 实现原理

### 雪花算法 (Snowflake)

雪花算法使用64位长整数表示一个ID，结构如下：

- 1位符号位，始终为0
- 41位时间戳，精确到毫秒
- 10位工作机器ID（5位数据中心ID + 5位工作节点ID）
- 12位序列号，同一毫秒内的序列号

优点：
- 生成速度快，不依赖外部系统
- ID趋势递增，适合作为数据库主键
- 分布式环境下全局唯一

注意事项：
- 需要解决时钟回拨问题
- 需要保证机器ID唯一

### 号段模式 (Segment)

号段模式基于数据库表预先分配一段ID区间给应用使用，用完后再次申请。

核心表结构：
```sql
CREATE TABLE easy_id_allocator (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  biz_key VARCHAR(128) NOT NULL COMMENT '业务键',
  max_id BIGINT(20) NOT NULL COMMENT '当前最大ID',
  step INT NOT NULL COMMENT '步长',
  version INT NOT NULL COMMENT '版本号',
  description VARCHAR(256) DEFAULT NULL COMMENT '描述',
  update_time TIMESTAMP NOT NULL COMMENT '更新时间',
  create_time TIMESTAMP NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_key (biz_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID分配表';
```

双Buffer机制：
- 每个业务维护两个号段，轮流使用
- 当前号段使用量到达阈值时，异步加载下一个号段
- 当前号段用尽时，切换到下一个号段继续使用

优点：
- ID连续性好，方便业务使用
- 可以为不同业务定制不同的ID规则
- 批量获取ID效率高

### Redis号段模式

基于Redis实现的号段模式，使用Lua脚本保证原子性操作：

```lua
local key = KEYS[1];
local step = tonumber(ARGV[1]);
local maxIdKey = key .. ':maxId';
local stepKey = key .. ':step';
local currentMaxId = tonumber(redis.call('get', maxIdKey)) or 0;
local currentStep = tonumber(redis.call('get', stepKey)) or step;
if step <= 0 then step = currentStep; end;
local nextMaxId = currentMaxId + step;
redis.call('set', maxIdKey, nextMaxId);
if currentStep ~= step and step > 0 then redis.call('set', stepKey, step); end;
return currentMaxId;
```

优点：
- 比数据库号段模式性能更好
- 不依赖关系型数据库
- 依然保持ID的连续性

## 监控与统计

Easy-ID内置了监控功能，记录以下指标：

- ID生成次数
- 成功/失败次数
- 平均响应时间
- 最大响应时间

监控数据会每分钟记录到日志中，方便查看和分析：

```
2023-03-15 12:34:56 INFO [easy-id-monitor] - ID生成器[snowflake]统计: 调用次数=10000, 成功次数=10000, 失败次数=0, 平均耗时=0.05ms, 最大耗时=3ms
```

## 使用建议

1. **选择合适的ID生成策略**
   - 对ID连续性要求高的场景，选择号段模式
   - 高并发、低延迟场景，选择雪花算法
   - 需要高性能且ID连续的场景，选择Redis号段模式

2. **雪花算法的部署**
   - 确保每个节点的workerId和dataCenterId唯一
   - 建议通过配置中心或服务注册发现组件动态分配

3. **号段模式的优化**
   - 适当调整步长，减少数据库访问频率
   - 根据业务量调整加载阈值，确保ID不会用尽
   - 关键业务可以预先初始化号段配置

4. **性能调优**
   - 雪花算法：调整序列位数
   - 号段模式：调整步长和加载阈值
   - Redis号段模式：优化Redis配置，使用Redis集群

## 常见问题

1. **ID生成速度慢怎么办？**  
   检查系统时间同步、调整步长、优化数据库连接池配置或考虑切换到性能更高的生成器类型。

2. **如何在分布式环境中保证雪花算法的机器ID唯一？**  
   可以通过配置中心集中管理，或使用ZooKeeper等组件动态分配。

3. **号段用完后未及时加载怎么办？**  
   调整加载阈值，例如设置为20%，当剩余20%的号段时开始异步加载下一个号段。

4. **时钟回拨问题如何解决？**  
   雪花算法内置了简单的时钟回拨处理机制，对于短时间内的回拨会等待，超过阈值则抛出异常。
