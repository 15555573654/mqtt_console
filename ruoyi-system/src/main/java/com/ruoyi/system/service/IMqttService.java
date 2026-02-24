package com.ruoyi.system.service;

import java.util.Map;

/**
 * MQTT服务接口
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
public interface IMqttService
{
    /**
     * 连接到MQTT服务器
     *
     * @param username 用户名
     * @param password 密码
     * @return 是否连接成功
     */
    boolean connect(String username, String password);

    /**
     * 断开MQTT连接
     */
    void disconnect();

    /**
     * 检查是否已连接
     * 
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 发布命令到设备
     * 
     * @param deviceName 设备名称
     * @param action 操作类型
     * @param params 参数
     * @return 是否发送成功
     */
    boolean publishCommand(String deviceName, String action, Map<String, Object> params);

    /**
     * 发布任务配置到设备
     * 
     * @param deviceName 设备名称
     * @param configContent 配置内容
     * @return 是否发送成功
     */
    boolean publishTaskConfig(String deviceName, String configContent);

    /**
     * 订阅设备主题
     * 
     * @param username 用户名
     */
    void subscribeToDeviceTopics(String username);

    /**
     * 发送自定义命令到设备
     * 
     * @param deviceName 设备名称
     * @param commandType 命令类型
     * @param message 消息内容
     * @return 是否发送成功
     */
    boolean sendCustomCommand(String deviceName, int commandType, String message);

    /**
     * 删除设备（清除MQTT主题）
     * 
     * @param deviceName 设备名称
     * @return 是否删除成功
     */
    boolean deleteDevice(String deviceName);

    /**
     * 获取当前用户名
     * 
     * @return 用户名
     */
    String getCurrentUsername();
}
