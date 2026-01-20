-- MQTT云控菜单和权限配置
-- 注意：menu_id需要根据实际数据库中的最大值进行调整

-- 1. 添加MQTT云控主菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('MQTT云控', 0, 5, 'mqtt', NULL, 1, 0, 'M', '0', '0', NULL, 'monitor', 'admin', sysdate(), '', NULL, 'MQTT云控管理');

-- 获取刚插入的主菜单ID（假设为2000，实际使用时需要查询）
SET @mqtt_parent_id = LAST_INSERT_ID();

-- 2. 添加设备管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('设备管理', @mqtt_parent_id, 1, 'device', 'mqtt/device/index', 1, 0, 'C', '0', '0', 'mqtt:device:list', 'monitor', 'admin', sysdate(), '', NULL, 'MQTT设备管理');

SET @device_menu_id = LAST_INSERT_ID();

-- 设备管理子权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
('设备查询', @device_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'mqtt:device:query', '#', 'admin', sysdate(), '', NULL, ''),
('设备删除', @device_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'mqtt:device:remove', '#', 'admin', sysdate(), '', NULL, ''),
('设备命令', @device_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'mqtt:device:command', '#', 'admin', sysdate(), '', NULL, '');

-- 3. 添加任务配置菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('任务配置', @mqtt_parent_id, 2, 'config', 'mqtt/config/index', 1, 0, 'C', '0', '0', 'mqtt:config:list', 'edit', 'admin', sysdate(), '', NULL, 'MQTT任务配置');

SET @config_menu_id = LAST_INSERT_ID();

-- 任务配置子权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
('配置查询', @config_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'mqtt:config:query', '#', 'admin', sysdate(), '', NULL, ''),
('配置新增', @config_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'mqtt:config:add', '#', 'admin', sysdate(), '', NULL, ''),
('配置修改', @config_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'mqtt:config:edit', '#', 'admin', sysdate(), '', NULL, ''),
('配置删除', @config_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'mqtt:config:remove', '#', 'admin', sysdate(), '', NULL, ''),
('配置下发', @config_menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'mqtt:config:send', '#', 'admin', sysdate(), '', NULL, '');

-- 4. 添加连接设置菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('连接设置', @mqtt_parent_id, 3, 'connection', 'mqtt/connection/index', 1, 0, 'C', '0', '0', 'mqtt:connection:list', 'link', 'admin', sysdate(), '', NULL, 'MQTT连接设置');

SET @connection_menu_id = LAST_INSERT_ID();

-- 连接设置子权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
('连接查询', @connection_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'mqtt:connection:query', '#', 'admin', sysdate(), '', NULL, ''),
('连接操作', @connection_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'mqtt:connection:connect', '#', 'admin', sysdate(), '', NULL, ''),
('断开连接', @connection_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'mqtt:connection:disconnect', '#', 'admin', sysdate(), '', NULL, '');

-- 5. 添加操作日志菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('操作日志', @mqtt_parent_id, 4, 'log', 'mqtt/log/index', 1, 0, 'C', '0', '0', 'mqtt:log:list', 'log', 'admin', sysdate(), '', NULL, 'MQTT操作日志');

SET @log_menu_id = LAST_INSERT_ID();

-- 操作日志子权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
('日志查询', @log_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'mqtt:log:query', '#', 'admin', sysdate(), '', NULL, ''),
('日志删除', @log_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'mqtt:log:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 6. 为管理员角色分配所有MQTT权限（假设管理员角色ID为1）
-- 注意：需要根据实际插入的menu_id进行调整
-- 可以通过以下查询获取所有MQTT相关的menu_id：
-- SELECT menu_id FROM sys_menu WHERE menu_name LIKE 'MQTT%' OR menu_name LIKE '%设备%' OR menu_name LIKE '%任务%' OR menu_name LIKE '%连接%' OR parent_id IN (SELECT menu_id FROM sys_menu WHERE menu_name = 'MQTT云控');

-- 示例：为角色ID=1的管理员分配权限（实际使用时需要根据查询结果调整）
-- INSERT INTO sys_role_menu (role_id, menu_id) 
-- SELECT 1, menu_id FROM sys_menu WHERE menu_name LIKE '%MQTT%' OR parent_id IN (SELECT menu_id FROM sys_menu WHERE menu_name = 'MQTT云控');
