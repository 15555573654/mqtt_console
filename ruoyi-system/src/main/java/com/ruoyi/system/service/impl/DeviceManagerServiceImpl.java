package com.ruoyi.system.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.MqttDevice;
import com.ruoyi.system.domain.vo.MqttDeviceStatistics;
import com.ruoyi.system.mapper.MqttDeviceMapper;
import com.ruoyi.system.service.IDeviceManagerService;

/**
 * 设备管理服务实现
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
@Service
public class DeviceManagerServiceImpl implements IDeviceManagerService
{
    @Autowired
    private MqttDeviceMapper mqttDeviceMapper;

    @Override
    public List<MqttDevice> selectMqttDeviceList(MqttDevice mqttDevice)
    {
        List<MqttDevice> devices = mqttDeviceMapper.selectMqttDeviceList(mqttDevice);
        
        // 动态计算设备状态和脚本状态
        long currentTime = System.currentTimeMillis();
        long heartbeatTimeout = 15000; // 15秒超时
        
        for (MqttDevice device : devices)
        {
            // 如果超过心跳超时时间，设置为离线
            if (device.getLastOnline() != null)
            {
                long timeSinceLastOnline = currentTime - device.getLastOnline().getTime();
                
                if (timeSinceLastOnline > heartbeatTimeout)
                {
                    device.setDeviceStatus("离线");
                    device.setScriptStatus("未运行");
                }
            }
        }
        
        return devices;
    }

    @Override
    public MqttDevice selectMqttDeviceByDeviceId(Long deviceId)
    {
        MqttDevice device = mqttDeviceMapper.selectMqttDeviceByDeviceId(deviceId);

        // 动态计算设备状态
        if (device != null && device.getLastOnline() != null)
        {
            long currentTime = System.currentTimeMillis();
            long heartbeatTimeout = 15000; // 15秒超时
            long timeSinceLastOnline = currentTime - device.getLastOnline().getTime();

            if (timeSinceLastOnline > heartbeatTimeout)
            {
                device.setDeviceStatus("离线");
                device.setScriptStatus("未运行");
            }
        }

        return device;
    }

    @Override
    public MqttDevice selectMqttDeviceByName(String deviceName)
    {
        MqttDevice query = new MqttDevice();
        query.setDeviceName(deviceName);
        List<MqttDevice> devices = mqttDeviceMapper.selectMqttDeviceList(query);

        if (devices != null && !devices.isEmpty())
        {
            return devices.get(0);
        }

        return null;
    }

    @Override
    public int insertMqttDevice(MqttDevice mqttDevice)
    {
        return mqttDeviceMapper.insertMqttDevice(mqttDevice);
    }

    @Override
    public int updateMqttDevice(MqttDevice mqttDevice)
    {
        return mqttDeviceMapper.updateMqttDevice(mqttDevice);
    }

    @Override
    public int deleteMqttDeviceByDeviceIds(Long[] deviceIds)
    {
        return mqttDeviceMapper.deleteMqttDeviceByDeviceIds(deviceIds);
    }

    @Override
    public int deleteMqttDeviceByDeviceId(Long deviceId)
    {
        return mqttDeviceMapper.deleteMqttDeviceByDeviceId(deviceId);
    }

    @Override
    public MqttDeviceStatistics getStatistics(String username)
    {
        MqttDevice query = new MqttDevice();
        if (username != null && !username.isEmpty())
        {
            query.setUsername(username);
        }
        List<MqttDevice> devices = mqttDeviceMapper.selectMqttDeviceList(query);

        int totalDevices = devices.size();
        int onlineDevices = 0;
        long totalDiamonds = 0L;
        
        // 动态计算在线设备数和钻石总数
        long currentTime = System.currentTimeMillis();
        long heartbeatTimeout = 15000; // 15秒超时
        
        for (MqttDevice device : devices)
        {
            boolean isOnline = false;
            
            // 判断设备是否在线
            if (device.getLastOnline() != null)
            {
                long timeSinceLastOnline = currentTime - device.getLastOnline().getTime();
                isOnline = timeSinceLastOnline <= heartbeatTimeout;
            }
            
            if (isOnline)
            {
                onlineDevices++;
            }
            
            // 累计钻石数（只统计数字）
            if (device.getDiamonds() != null && device.getDiamonds().matches("^[0-9]+$"))
            {
                try
                {
                    totalDiamonds += Long.parseLong(device.getDiamonds());
                }
                catch (NumberFormatException e)
                {
                    // 忽略无法解析的钻石数
                }
            }
        }
        
        int offlineDevices = totalDevices - onlineDevices;

        return new MqttDeviceStatistics(totalDevices, onlineDevices, offlineDevices, totalDiamonds);
    }
}
