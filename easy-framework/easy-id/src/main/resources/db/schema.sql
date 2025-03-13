-- 分布式ID生成器表
CREATE TABLE IF NOT EXISTS `id_generator` (
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