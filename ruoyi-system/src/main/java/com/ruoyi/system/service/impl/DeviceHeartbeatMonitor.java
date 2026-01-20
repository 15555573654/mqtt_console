package com.ruoyi.system.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ruoyi.system.domain.MqttDevice;
import com.ruoyi.system.mapper.MqttDeviceMapper;

/**
 * 设备心跳监控
 * 定期检查设备最后在线时间，超时则标记为离线
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
@Component
public class DeviceHeartbeatMonitor
{
    private static final Logger log = LoggerFactory.getLogger(DeviceHeartbeatMonitor.class);

    @Autowired
    private MqttDeviceMapper mqttDeviceMapper;

    /**
     * 心跳超时时间（毫秒）：15秒
     */
    private static final long HEARTBEAT_TIMEOUT = 15000;

    /**
     * 每5秒检查一次设备心跳
     */
    @Scheduled(fixedRate = 5000)
    public void checkDeviceHeartbeat()
    {
        try
        {
            // 查询所有在线设备
            MqttDevice query = new MqttDevice();
            query.setDeviceStatus("在线");
            List<MqttDevice> onlineDevices = mqttDeviceMapper.selectMqttDeviceList(query);

            long currentTime = System.currentTimeMillis();
            int offlineCount = 0;

            for (MqttDevice device : onlineDevices)
            {
                if (device.getLastOnline() != null)
                {
                    long lastOnlineTime = device.getLastOnline().getTime();
                    long timeSinceLastOnline = currentTime - lastOnlineTime;

                    // 如果超过心跳超时时间，标记为离线
                    if (timeSinceLastOnline > HEARTBEAT_TIMEOUT)
                    {
                        device.setDeviceStatus("离线");
                        mqttDeviceMapper.updateMqttDevice(device);
                        offlineCount++;
                        log.info("设备心跳超时，标记为离线: deviceName={}, lastOnline={}, timeout={}ms", 
                            device.getDeviceName(), device.getLastOnline(), timeSinceLastOnline);
                    }
                }
            }

            if (offlineCount > 0)
            {
                log.info("心跳检查完成，标记{}个设备为离线", offlineCount);
            }
        }
        catch (Exception e)
        {
            log.error("设备心跳检查失败", e);
        }
    }
}
