-- MQTT云控系统数据库表
-- 作者: Kiro
-- 日期: 2025-01-19

-- ----------------------------
-- 1. MQTT设备表
-- ----------------------------
DROP TABLE IF EXISTS `mqtt_device`;
CREATE TABLE `mqtt_device` (
  `device_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称',
  `username` VARCHAR(100) NOT NULL COMMENT '所属用户',
  `device_status` VARCHAR(20) DEFAULT '离线' COMMENT '设备状态(在线/离线)',
  `script_status` VARCHAR(20) DEFAULT '未运行' COMMENT '脚本状态(运行中/未运行/暂停)',
  `level` VARCHAR(50) DEFAULT NULL COMMENT '等级',
  `server` VARCHAR(100) DEFAULT NULL COMMENT '区服',
  `diamonds` VARCHAR(50) DEFAULT NULL COMMENT '钻石数量',
  `task_config` VARCHAR(100) DEFAULT NULL COMMENT '任务配置名称',
  `last_online` DATETIME DEFAULT NULL COMMENT '最后在线时间',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`device_id`),
  UNIQUE KEY `uk_device_username` (`device_name`, `username`),
  KEY `idx_username` (`username`),
  KEY `idx_status` (`device_status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='MQTT设备信息表';

-- ----------------------------
-- 2. MQTT任务配置表
-- ----------------------------
DROP TABLE IF EXISTS `mqtt_task_config`;
CREATE TABLE `mqtt_task_config` (
  `config_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
  `config_content` TEXT NOT NULL COMMENT '配置内容(JSON格式)',
  `username` VARCHAR(100) NOT NULL COMMENT '所属用户',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`),
  KEY `idx_username` (`username`),
  KEY `idx_name` (`config_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='MQTT任务配置表';

-- ----------------------------
-- 3. MQTT连接配置表
-- ----------------------------
DROP TABLE IF EXISTS `mqtt_connection_config`;
CREATE TABLE `mqtt_connection_config` (
  `config_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `username` VARCHAR(100) NOT NULL COMMENT '用户名',
  `mqtt_host` VARCHAR(255) NOT NULL COMMENT 'MQTT服务器地址',
  `mqtt_port` INT(11) NOT NULL DEFAULT 1883 COMMENT 'MQTT服务器端口',
  `mqtt_username` VARCHAR(100) NOT NULL COMMENT 'MQTT用户名',
  `mqtt_password` VARCHAR(255) NOT NULL COMMENT 'MQTT密码(加密)',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用(0否 1是)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='MQTT连接配置表';

-- ----------------------------
-- 4. MQTT操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `mqtt_operation_log`;
CREATE TABLE `mqtt_operation_log` (
  `log_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `username` VARCHAR(100) NOT NULL COMMENT '操作用户',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `device_names` TEXT COMMENT '设备名称列表',
  `operation_content` TEXT COMMENT '操作内容',
  `result` VARCHAR(20) DEFAULT NULL COMMENT '操作结果(成功/失败)',
  `error_message` TEXT COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_username` (`username`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='MQTT操作日志表';

-- ----------------------------
-- 初始化数据
-- ----------------------------
-- 插入默认MQTT连接配置
INSERT INTO `mqtt_connection_config` (`username`, `mqtt_host`, `mqtt_port`, `mqtt_username`, `mqtt_password`, `enabled`) 
VALUES ('admin', '192.168.1.104', 1883, 'test002', 'test003', 1);
