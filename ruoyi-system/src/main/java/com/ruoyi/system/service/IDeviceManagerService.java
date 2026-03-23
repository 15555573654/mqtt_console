package com.ruoyi.system.service;

import java.util.List;

import com.ruoyi.system.domain.MqttDevice;
import com.ruoyi.system.domain.vo.MqttDeviceStatistics;

/**
 * 设备管理服务接口
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
public interface IDeviceManagerService
{
    /**
     * 查询设备列表
     * 
     * @param mqttDevice 设备信息
     * @return 设备集合
     */
    List<MqttDevice> selectMqttDeviceList(MqttDevice mqttDevice);

    /**
     * 查询设备
     *
     * @param deviceId 设备ID
     * @return 设备对象
     */
    MqttDevice selectMqttDeviceByDeviceId(Long deviceId);

    /**
     * 根据设备名称查询设备
     *
     * @param deviceName 设备名称
     * @return 设备对象
     */
    MqttDevice selectMqttDeviceByName(String deviceName);

    /**
     * 新增设备
     * 
     * @param mqttDevice 设备信息
     * @return 结果
     */
    int insertMqttDevice(MqttDevice mqttDevice);

    /**
     * 修改设备
     * 
     * @param mqttDevice 设备信息
     * @return 结果
     */
    int updateMqttDevice(MqttDevice mqttDevice);

    /**
     * 批量删除设备
     * 
     * @param deviceIds 需要删除的设备ID
     * @return 结果
     */
    int deleteMqttDeviceByDeviceIds(Long[] deviceIds);

    /**
     * 删除设备信息
     * 
     * @param deviceId 设备ID
     * @return 结果
     */
    int deleteMqttDeviceByDeviceId(Long deviceId);

    /**
     * 获取设备统计信息
     * 
     * @param username 用户名
     * @return 统计信息
     */
    MqttDeviceStatistics getStatistics(String username);
}
