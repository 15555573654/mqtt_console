package com.ruoyi.system.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.system.service.IMqttMessageHandler;
import com.ruoyi.system.service.IMqttService;

/**
 * MQTT服务实现
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
@Service
public class MqttServiceImpl implements IMqttService
{
    private static final Logger log = LoggerFactory.getLogger(MqttServiceImpl.class);

    @Autowired(required = false)
    private IMqttMessageHandler messageHandler;

    private MqttClient mqttClient;
    private String username;
    private String password;
    private String host = "tcp://192.168.1.104:1883";
    private boolean isConnected = false;
    private boolean isClosing = false;

    // MQTT主题
    private String returnTopic;
    private String statusTopic;
    private String commandTopic;
    private String scriptMessageTopic;

    @Override
    public boolean connect(String username, String password)
    {
        try
        {
            this.username = username;
            this.password = password;
            updateTopics();

            // 如果已有连接，先断开
            if (mqttClient != null && mqttClient.isConnected())
            {
                mqttClient.disconnect();
                mqttClient.close();
            }

            // 创建MQTT客户端（使用内存持久化，不生成文件）
            String clientId = "RuoYi_MQTT_" + username + "_" + System.currentTimeMillis();
            mqttClient = new MqttClient(host, clientId, new MemoryPersistence());

            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setCleanSession(true);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);

            // 设置回调
            mqttClient.setCallback(new MqttCallback()
            {
                @Override
                public void connectionLost(Throwable cause)
                {
                    log.warn("MQTT连接断开: {}", cause.getMessage());
                    isConnected = false;
                    if (!isClosing)
                    {
                        log.info("尝试重新连接MQTT...");
                        reconnect();
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception
                {
                    handleMessage(topic, new String(message.getPayload(), "UTF-8"));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token)
                {
                    // 消息发送完成
                }
            });

            // 连接到MQTT服务器
            log.info("连接MQTT服务器: {}", host);
            mqttClient.connect(options);
            isConnected = true;

            // 订阅主题
            subscribeToDeviceTopics(username);

            log.info("MQTT连接成功");
            return true;
        }
        catch (Exception e)
        {
            log.error("MQTT连接失败", e);
            isConnected = false;
            return false;
        }
    }

    @Override
    public void disconnect()
    {
        isClosing = true;
        if (mqttClient != null)
        {
            try
            {
                if (mqttClient.isConnected())
                {
                    mqttClient.disconnect();
                }
                mqttClient.close();
                log.info("MQTT连接已断开");
            }
            catch (Exception e)
            {
                log.error("断开MQTT连接时出错", e);
            }
        }
        isConnected = false;
    }

    @Override
    public boolean isConnected()
    {
        return isConnected && mqttClient != null && mqttClient.isConnected();
    }

    @Override
    public boolean publishCommand(String deviceName, String action, Map<String, Object> params)
    {
        try
        {
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("action", action);
            if (params != null)
            {
                commandData.putAll(params);
            }

            String topic = commandTopic + deviceName;
            String message = JSON.toJSONString(commandData);

            return publish(topic, message, 1);
        }
        catch (Exception e)
        {
            log.error("发送命令失败: deviceName={}, action={}", deviceName, action, e);
            return false;
        }
    }

    @Override
    public boolean publishTaskConfig(String deviceName, String configContent)
    {
        try
        {
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("action", "setTaskConfig");
            commandData.put("taskConfig", configContent);

            String topic = commandTopic + deviceName;
            String message = JSON.toJSONString(commandData);

            return publish(topic, message, 1);
        }
        catch (Exception e)
        {
            log.error("发送任务配置失败: deviceName={}", deviceName, e);
            return false;
        }
    }

    @Override
    public void subscribeToDeviceTopics(String username)
    {
        try
        {
            if (mqttClient != null && mqttClient.isConnected())
            {
                mqttClient.subscribe(returnTopic, 1);
                mqttClient.subscribe(statusTopic, 1);
                mqttClient.subscribe(scriptMessageTopic, 1);

                log.info("订阅主题: {}", returnTopic);
                log.info("订阅主题: {}", statusTopic);
                log.info("订阅主题: {}", scriptMessageTopic);
            }
        }
        catch (Exception e)
        {
            log.error("订阅主题失败", e);
        }
    }

    @Override
    public boolean sendCustomCommand(String deviceName, int commandType, String message)
    {
        try
        {
            Map<String, Object> params = new HashMap<>();
            params.put("messageType", commandType);
            params.put("message", message);

            Map<String, Object> commandData = new HashMap<>();
            commandData.put("action", "msgToScript");
            commandData.put("params", params);

            String topic = commandTopic + deviceName;
            String jsonMessage = JSON.toJSONString(commandData);

            return publish(topic, jsonMessage, 1);
        }
        catch (Exception e)
        {
            log.error("发送自定义命令失败: deviceName={}", deviceName, e);
            return false;
        }
    }

    @Override
    public boolean deleteDevice(String deviceName)
    {
        try
        {
            String statusTopicPath = "status/" + username + "/" + deviceName;
            String configTopicPath = "config/" + username + "/" + deviceName;

            // 发送空消息清除主题（retain=true）
            publish(statusTopicPath, "", 0, true);
            publish(configTopicPath, "", 0, true);

            log.info("删除设备MQTT主题: {}", deviceName);
            return true;
        }
        catch (Exception e)
        {
            log.error("删除设备MQTT主题失败: deviceName={}", deviceName, e);
            return false;
        }
    }

    @Override
    public String getCurrentUsername()
    {
        return username;
    }

    /**
     * 发布MQTT消息
     */
    private boolean publish(String topic, String message, int qos)
    {
        return publish(topic, message, qos, false);
    }

    /**
     * 发布MQTT消息
     */
    private boolean publish(String topic, String message, int qos, boolean retained)
    {
        if (mqttClient != null && mqttClient.isConnected())
        {
            try
            {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes("UTF-8"));
                mqttMessage.setQos(qos);
                mqttMessage.setRetained(retained);
                mqttClient.publish(topic, mqttMessage);
                return true;
            }
            catch (Exception e)
            {
                log.error("发布消息失败: topic={}", topic, e);
                return false;
            }
        }
        return false;
    }

    /**
     * 处理接收到的MQTT消息
     */
    private void handleMessage(String topic, String payload)
    {
        try
        {
            log.info("收到MQTT消息: topic={}, payload={}", topic, payload);

            if (messageHandler != null)
            {
                log.info("调用消息处理器处理消息");
                messageHandler.handleMessage(topic, payload);
            }
            else
            {
                log.error("消息处理器为null，无法处理消息！");
            }
        }
        catch (Exception e)
        {
            log.error("处理MQTT消息失败: topic={}", topic, e);
        }
    }

    /**
     * 更新MQTT主题
     */
    private void updateTopics()
    {
        this.returnTopic = "response/" + username + "/#";
        this.statusTopic = "status/" + username + "/#";
        this.commandTopic = "commands/" + username + "/";
        this.scriptMessageTopic = "config/" + username + "/#";
    }

    /**
     * 重新连接
     */
    private void reconnect()
    {
        new Thread(() -> {
            int retryCount = 0;
            int maxRetries = 10;
            int retryInterval = 3000; // 3秒

            while (retryCount < maxRetries && !isClosing)
            {
                try
                {
                    Thread.sleep(retryInterval);
                    log.info("尝试重新连接MQTT (第{}次)...", retryCount + 1);

                    if (mqttClient != null && !mqttClient.isConnected())
                    {
                        mqttClient.reconnect();
                        isConnected = true;
                        log.info("MQTT重新连接成功");
                        break;
                    }
                }
                catch (Exception e)
                {
                    log.error("MQTT重新连接失败 (第{}次): {}", retryCount + 1, e.getMessage());
                    retryCount++;
                }
            }
        }).start();
    }
}
