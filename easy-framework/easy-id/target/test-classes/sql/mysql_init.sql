-- 创建ID分配表（如果不存在）
CREATE TABLE IF NOT EXISTS `easy_id_allocator` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_key` VARCHAR(128) NOT NULL COMMENT '业务键',
  `max_id` BIGINT NOT NULL COMMENT '当前最大ID',
  `step` INT NOT NULL COMMENT '步长',
  `version` INT NOT NULL COMMENT '版本号',
  `description` VARCHAR(256) DEFAULT NULL COMMENT '描述',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_key` (`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID分配表';

-- 清空表数据（如果已存在）
TRUNCATE TABLE `easy_id_allocator`;

-- 插入初始数据
INSERT INTO `easy_id_allocator` (`biz_key`, `max_id`, `step`, `version`, `description`) VALUES 
('default', 0, 1000, 1, '默认业务'),
('user_id', 10000, 1000, 1, '用户ID'),
('order_id', 100000, 2000, 1, '订单ID'),
('product_id', 1000, 500, 1, '商品ID'),
('payment_id', 5000, 1000, 1, '支付ID'); 