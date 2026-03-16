package com.ruoyi.screencast.mqtt

import android.content.Context
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttManager(private val context: Context) {
    
    private var mqttClient: MqttClient? = null
    private var statusCallback: ((String) -> Unit)? = null
    private var logCallback: ((String) -> Unit)? = null
    private var messageCallback: ((String, String) -> Unit)? = null
    
    private var username: String = ""
    private var deviceName: String = ""
    
    fun setStatusCallback(callback: (String) -> Unit) {
        statusCallback = callback
    }
    
    fun setLogCallback(callback: (String) -> Unit) {
        logCallback = callback
    }
    
    fun setMessageCallback(callback: (String, String) -> Unit) {
        messageCallback = callback
    }
    
    fun connect(host: String, port: Int, user: String, pass: String, device: String, callback: (Boolean) -> Unit) {
        username = user
        deviceName = device
        
        try {
            val broker = "tcp://$host:$port"
            val clientId = "android_${System.currentTimeMillis()}"
            
            logCallback?.invoke("正在连接MQTT: $broker")
            statusCallback?.invoke("连接中...")
            
            mqttClient = MqttClient(broker, clientId, MemoryPersistence())
            
            val options = MqttConnectOptions().apply {
                userName = user
                password = pass.toCharArray()
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 60
            }
            
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    logCallback?.invoke("MQTT连接丢失: ${cause?.message}")
                    statusCallback?.invoke("连接丢失")
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val msg = message?.toString() ?: return
                    logCallback?.invoke("收到消息: $topic")
                    messageCallback?.invoke(topic ?: "", msg)
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                }
            })
            
            mqttClient?.connect(options)
            
            val topic = "webrtc/$username/$deviceName"
            mqttClient?.subscribe(topic, 1)
            
            logCallback?.invoke("MQTT连接成功")
            logCallback?.invoke("已订阅主题: $topic")
            statusCallback?.invoke("已连接")
            callback(true)
            
        } catch (e: Exception) {
            logCallback?.invoke("MQTT连接失败: ${e.message}")
            statusCallback?.invoke("连接失败")
            callback(false)
        }
    }
    
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
            mqttClient = null
            logCallback?.invoke("MQTT已断开")
            statusCallback?.invoke("未连接")
        } catch (e: Exception) {
            logCallback?.invoke("断开失败: ${e.message}")
        }
    }
    
    fun publish(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttMessage.qos = 1
            mqttClient?.publish(topic, mqttMessage)
        } catch (e: Exception) {
            logCallback?.invoke("发送消息失败: ${e.message}")
        }
    }
    
    fun isConnected(): Boolean {
        return mqttClient?.isConnected ?: false
    }
    
    fun getDeviceName(): String = deviceName
    fun getUsername(): String = username
}
