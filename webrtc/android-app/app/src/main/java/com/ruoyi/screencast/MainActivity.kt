package com.ruoyi.screencast

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ruoyi.screencast.databinding.ActivityMainBinding
import com.ruoyi.screencast.service.ScreenCaptureService
import com.ruoyi.screencast.webrtc.WebRTCManager
import com.ruoyi.screencast.mqtt.MqttManager
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var mqttManager: MqttManager
    private lateinit var webrtcManager: WebRTCManager
    
    private val SCREEN_CAPTURE_REQUEST = 1001
    private val PERMISSION_REQUEST = 1002
    
    // 保存屏幕捕获的 Intent，用于自动启动
    private var pendingScreenCaptureData: Intent? = null
    private var pendingScreenCaptureResultCode: Int = 0
    private var shouldAutoStartCapture = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        mqttManager = MqttManager(this)
        webrtcManager = WebRTCManager(this)
        
        setupListeners()
        checkPermissions()
        
        // 应用启动时就请求屏幕捕获权限
        requestScreenCapturePermissionOnStartup()
        
        log("应用启动成功")
    }
    
    private fun setupListeners() {
        binding.btnConnect.setOnClickListener {
            connectMqtt()
        }
        
        binding.btnDisconnect.setOnClickListener {
            disconnectMqtt()
        }
        
        binding.btnStartScreen.setOnClickListener {
            if (pendingScreenCaptureData != null) {
                // 已有权限，直接启动
                startScreenCaptureWithPermission()
            } else {
                // 没有权限，请求权限
                requestScreenCapture()
            }
        }
        
        binding.btnEnableAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
        
        binding.btnClearLog.setOnClickListener {
            binding.tvLog.text = ""
        }
        
        mqttManager.setStatusCallback { status ->
            runOnUiThread {
                binding.tvMqttStatus.text = "状态: $status"
            }
        }
        
        mqttManager.setLogCallback { message ->
            log(message)
        }
        
        webrtcManager.setStatusCallback { status ->
            runOnUiThread {
                binding.tvScreenStatus.text = "状态: $status"
            }
        }
        
        webrtcManager.setLogCallback { message ->
            log(message)
        }
        
        // 设置屏幕捕获请求回调
        webrtcManager.setScreenCaptureRequestCallback {
            runOnUiThread {
                if (pendingScreenCaptureData != null) {
                    // 已有权限，直接启动
                    startScreenCaptureWithPermission()
                    log("收到远程投屏请求，自动启动屏幕捕获")
                } else {
                    // 没有权限，提示用户
                    Toast.makeText(this, "请先授权屏幕录制权限", Toast.LENGTH_SHORT).show()
                    requestScreenCapture()
                }
            }
        }
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        val needRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (needRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needRequest.toTypedArray(), PERMISSION_REQUEST)
        }
    }
    
    private fun connectMqtt() {
        val host = binding.etMqttHost.text.toString()
        val port = binding.etMqttPort.text.toString()
        val username = binding.etUsername.text.toString()
        val password = binding.etPassword.text.toString()
        val deviceName = binding.etDeviceName.text.toString()
        
        if (host.isEmpty() || port.isEmpty() || username.isEmpty() || password.isEmpty() || deviceName.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }
        
        mqttManager.connect(host, port.toInt(), username, password, deviceName) { success ->
            runOnUiThread {
                if (success) {
                    binding.btnConnect.isEnabled = false
                    binding.btnDisconnect.isEnabled = true
                    binding.btnStartScreen.isEnabled = true
                    
                    webrtcManager.setMqttManager(mqttManager)
                    webrtcManager.setDeviceInfo(username, deviceName)
                } else {
                    Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun disconnectMqtt() {
        mqttManager.disconnect()
        webrtcManager.release()
        binding.btnConnect.isEnabled = true
        binding.btnDisconnect.isEnabled = false
        binding.btnStartScreen.isEnabled = false
        log("已断开连接并清理资源")
    }
    
    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST)
    }
    
    private fun requestScreenCapturePermissionOnStartup() {
        // 延迟 2 秒后自动请求屏幕捕获权限
        binding.root.postDelayed({
            log("正在请求屏幕录制权限...")
            Toast.makeText(this, "请授权屏幕录制权限，以便远程投屏", Toast.LENGTH_LONG).show()
            requestScreenCapture()
        }, 2000)
    }
    
    private fun startScreenCaptureWithPermission() {
        if (pendingScreenCaptureData == null) {
            Toast.makeText(this, "请先授权屏幕录制权限", Toast.LENGTH_SHORT).show()
            return
        }
        
        val serviceIntent = Intent(this, ScreenCaptureService::class.java)
        serviceIntent.putExtra("resultCode", pendingScreenCaptureResultCode)
        serviceIntent.putExtra("data", pendingScreenCaptureData)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        webrtcManager.startCapture(pendingScreenCaptureResultCode, pendingScreenCaptureData!!)
        log("屏幕捕获已启动")
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "请启用 '若依投屏' 无障碍服务", Toast.LENGTH_LONG).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == SCREEN_CAPTURE_REQUEST && resultCode == RESULT_OK && data != null) {
            // 保存屏幕捕获权限，但不立即启动
            pendingScreenCaptureData = data
            pendingScreenCaptureResultCode = resultCode
            log("✓ 屏幕录制权限已授权，等待远程投屏请求")
            Toast.makeText(this, "权限已授权，可以开始使用", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun log(message: String) {
        runOnUiThread {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val logText = binding.tvLog.text.toString()
            binding.tvLog.text = "$logText\n[$time] $message"
            
            binding.tvLog.post {
                val scrollView = binding.tvLog.parent as? android.widget.ScrollView
                scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mqttManager.disconnect()
        webrtcManager.release()
    }
}
