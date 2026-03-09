<template>
  <div class="screencast-container">
    <!-- 顶部控制栏 -->
    <el-card class="control-panel">
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="device-selector">
            <el-form :inline="true" size="small">
              <el-form-item label="选择设备">
                <el-select v-model="selectedDevice" placeholder="请选择设备" @change="handleDeviceChange">
                  <el-option
                    v-for="device in onlineDevices"
                    :key="device.deviceName"
                    :label="device.deviceName"
                    :value="device.deviceName"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="el-icon-refresh" @click="refreshDevices">刷新</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="connection-status">
            <el-tag :type="connectionStatus === 'connected' ? 'success' : 'info'" size="medium">
              {{ connectionStatusText }}
            </el-tag>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 视频显示区域 -->
    <el-row :gutter="20" class="video-area">
      <el-col :span="16">
        <el-card class="video-card">
          <div slot="header" class="card-header">
            <span>远程屏幕</span>
            <div class="header-controls">
              <el-button-group>
                <el-button size="mini" icon="el-icon-full-screen" @click="toggleFullscreen">全屏</el-button>
                <el-button size="mini" icon="el-icon-picture" @click="captureScreenshot">截图</el-button>
              </el-button-group>
            </div>
          </div>
          <div class="video-wrapper" ref="videoWrapper">
            <video
              ref="remoteVideo"
              autoplay
              playsinline
              :class="{ 'fullscreen': isFullscreen }"
            ></video>
            <div v-if="!isStreaming" class="no-stream-placeholder">
              <i class="el-icon-video-camera"></i>
              <p>暂无视频流</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="control-card">
          <div slot="header">
            <span>投屏控制</span>
          </div>
          
          <!-- 连接控制 -->
          <div class="control-section">
            <h4>连接控制</h4>
            <el-button-group class="control-buttons">
              <el-button
                type="success"
                icon="el-icon-video-play"
                :disabled="!selectedDevice || connectionStatus === 'connected'"
                @click="startScreencast"
              >开始投屏</el-button>
              <el-button
                type="danger"
                icon="el-icon-video-pause"
                :disabled="connectionStatus !== 'connected'"
                @click="stopScreencast"
              >停止投屏</el-button>
            </el-button-group>
          </div>

          <!-- 摄像头控制 -->
          <div class="control-section">
            <h4>摄像头控制</h4>
            <el-select v-model="selectedCamera" placeholder="选择摄像头" size="small" style="width: 100%; margin-bottom: 10px">
              <el-option
                v-for="camera in availableCameras"
                :key="camera.deviceId"
                :label="camera.label || '摄像头 ' + camera.deviceId.substr(0, 8)"
                :value="camera.deviceId"
              />
            </el-select>
            <el-button-group class="control-buttons">
              <el-button
                size="small"
                icon="el-icon-refresh"
                @click="switchCamera"
                :disabled="!isStreaming"
              >切换摄像头</el-button>
              <el-button
                size="small"
                icon="el-icon-monitor"
                @click="requestScreenShare"
                :disabled="!selectedDevice"
              >请求屏幕共享</el-button>
            </el-button-group>
          </div>

          <!-- 视频设置 -->
          <div class="control-section">
            <h4>视频设置</h4>
            <el-form size="small" label-width="80px">
              <el-form-item label="分辨率">
                <el-select v-model="videoConstraints.resolution" @change="applyVideoSettings">
                  <el-option label="480x320" value="480x320" />
                  <el-option label="640x480" value="640x480" />
                  <el-option label="1280x720" value="1280x720" />
                  <el-option label="1920x1080" value="1920x1080" />
                </el-select>
              </el-form-item>
              <el-form-item label="帧率">
                <el-select v-model="videoConstraints.frameRate" @change="applyVideoSettings">
                  <el-option label="15 fps" :value="15" />
                  <el-option label="30 fps" :value="30" />
                  <el-option label="60 fps" :value="60" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>

          <!-- 统计信息 -->
          <div class="control-section">
            <h4>连接信息</h4>
            <div class="stats-info">
              <p><span>状态:</span> {{ connectionStatusText }}</p>
              <p><span>设备:</span> {{ selectedDevice || '未选择' }}</p>
              <p><span>分辨率:</span> {{ videoConstraints.resolution }}</p>
              <p><span>帧率:</span> {{ videoConstraints.frameRate }} fps</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { listDevice } from "@/api/mqtt/device";
import mqtt from 'mqtt';

export default {
  name: "Screencast",
  data() {
    return {
      // MQTT连接
      mqttClient: null,
      mqttConnected: false,
      
      // 设备列表
      onlineDevices: [],
      selectedDevice: null,
      
      // WebRTC连接
      peerConnection: null,
      dataChannel: null,
      connectionStatus: 'disconnected', // disconnected, connecting, connected
      
      // 视频流
      localStream: null,
      remoteStream: null,
      isStreaming: false,
      isFullscreen: false,
      
      // 摄像头
      availableCameras: [],
      selectedCamera: null,
      
      // 视频约束
      videoConstraints: {
        resolution: '640x480',
        frameRate: 30
      },
      
      // ICE服务器配置
      iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' }
      ]
    };
  },
  computed: {
    connectionStatusText() {
      const statusMap = {
        'disconnected': '未连接',
        'connecting': '连接中...',
        'connected': '已连接'
      };
      return statusMap[this.connectionStatus] || '未知';
    }
  },
  created() {
    this.initMqtt();
    this.refreshDevices();
    this.getMediaDevices();
    
    // 从URL参数获取设备名称
    if (this.$route.query.device) {
      this.selectedDevice = this.$route.query.device;
    }
  },
  beforeDestroy() {
    this.cleanup();
  },
  methods: {
    /** 初始化MQTT连接 */
    initMqtt() {
      const config = localStorage.getItem('mqttConnectionConfig');
      if (!config) {
        this.$message.warning('请先在设备管理页面连接MQTT');
        return;
      }
      
      try {
        const { username, password } = JSON.parse(config);
        const mqttHost = '192.168.1.132';
        const mqttPort = '8083';
        const url = `ws://${mqttHost}:${mqttPort}/mqtt`;
        
        const options = {
          clientId: 'mqtt_screencast_' + username + '_' + Math.random().toString(16).substr(2, 8),
          username: username,
          password: password,
          clean: true,
          reconnectPeriod: 1000
        };
        
        this.mqttClient = mqtt.connect(url, options);
        
        this.mqttClient.on('connect', () => {
          this.mqttConnected = true;
          console.log('MQTT连接成功');
          // 订阅WebRTC信令主题
          this.mqttClient.subscribe(`webrtc/${username}/#`, { qos: 1 });
        });
        
        this.mqttClient.on('message', (topic, message) => {
          this.handleMqttMessage(topic, message.toString());
        });
        
        this.mqttClient.on('error', (err) => {
          console.error('MQTT连接错误:', err);
          this.$message.error('MQTT连接失败');
        });
      } catch (e) {
        console.error('初始化MQTT失败:', e);
      }
    },
    
    /** 处理MQTT消息 */
    handleMqttMessage(topic, payload) {
      try {
        const data = JSON.parse(payload);
        
        // 处理WebRTC信令消息
        if (topic.includes('/webrtc/')) {
          this.handleSignaling(data);
        }
      } catch (e) {
        console.error('解析MQTT消息失败:', e);
      }
    },
    
    /** 处理WebRTC信令 */
    async handleSignaling(data) {
      const { type, deviceName } = data;
      
      if (deviceName !== this.selectedDevice) return;
      
      try {
        switch (type) {
          case 'offer':
            await this.handleOffer(data.offer);
            break;
          case 'answer':
            await this.handleAnswer(data.answer);
            break;
          case 'ice-candidate':
            await this.handleIceCandidate(data.candidate);
            break;
        }
      } catch (e) {
        console.error('处理信令失败:', e);
      }
    },
    
    /** 刷新设备列表 */
    refreshDevices() {
      listDevice({ deviceStatus: '在线' }).then(response => {
        this.onlineDevices = response.rows || [];
      });
    },
    
    /** 设备切换 */
    handleDeviceChange() {
      if (this.connectionStatus === 'connected') {
        this.$confirm('切换设备将断开当前连接，是否继续？', '提示', {
          type: 'warning'
        }).then(() => {
          this.stopScreencast();
        }).catch(() => {
          // 取消切换，恢复原选择
        });
      }
    },
    
    /** 获取媒体设备列表 */
    async getMediaDevices() {
      try {
        const devices = await navigator.mediaDevices.enumerateDevices();
        this.availableCameras = devices.filter(device => device.kind === 'videoinput');
        if (this.availableCameras.length > 0) {
          this.selectedCamera = this.availableCameras[0].deviceId;
        }
      } catch (e) {
        console.error('获取媒体设备失败:', e);
      }
    },
    
    /** 开始投屏 */
    async startScreencast() {
      if (!this.selectedDevice) {
        this.$message.warning('请先选择设备');
        return;
      }
      
      if (!this.mqttConnected) {
        this.$message.error('MQTT未连接');
        return;
      }
      
      try {
        this.connectionStatus = 'connecting';
        
        // 创建PeerConnection
        await this.createPeerConnection();
        
        // 创建DataChannel
        this.createDataChannel();
        
        // 创建Offer
        const offer = await this.peerConnection.createOffer();
        await this.peerConnection.setLocalDescription(offer);
        
        // 通过MQTT发送Offer
        this.sendSignaling({
          type: 'offer',
          deviceName: this.selectedDevice,
          offer: offer
        });
        
        this.$message.success('正在建立连接...');
      } catch (e) {
        console.error('开始投屏失败:', e);
        this.$message.error('开始投屏失败: ' + e.message);
        this.connectionStatus = 'disconnected';
      }
    },
    
    /** 停止投屏 */
    stopScreencast() {
      this.cleanup();
      this.connectionStatus = 'disconnected';
      this.isStreaming = false;
      this.$message.info('已停止投屏');
    },
    
    /** 创建PeerConnection */
    async createPeerConnection() {
      const configuration = {
        iceServers: this.iceServers
      };
      
      this.peerConnection = new RTCPeerConnection(configuration);
      
      // ICE候选事件
      this.peerConnection.onicecandidate = (event) => {
        if (event.candidate) {
          this.sendSignaling({
            type: 'ice-candidate',
            deviceName: this.selectedDevice,
            candidate: event.candidate
          });
        }
      };
      
      // 连接状态变化
      this.peerConnection.onconnectionstatechange = () => {
        console.log('连接状态:', this.peerConnection.connectionState);
        if (this.peerConnection.connectionState === 'connected') {
          this.connectionStatus = 'connected';
          this.$message.success('连接成功');
        } else if (this.peerConnection.connectionState === 'failed' || 
                   this.peerConnection.connectionState === 'disconnected') {
          this.connectionStatus = 'disconnected';
          this.isStreaming = false;
        }
      };
      
      // 接收远程流
      this.peerConnection.ontrack = (event) => {
        console.log('接收到远程流');
        this.remoteStream = event.streams[0];
        this.$refs.remoteVideo.srcObject = this.remoteStream;
        this.isStreaming = true;
      };
      
      // DataChannel接收
      this.peerConnection.ondatachannel = (event) => {
        const channel = event.channel;
        channel.onmessage = (e) => {
          this.handleDataChannelMessage(e.data);
        };
      };
    },
    
    /** 创建DataChannel */
    createDataChannel() {
      const dataChannelInit = {
        ordered: true,
        negotiated: true,
        id: 0
      };
      
      this.dataChannel = this.peerConnection.createDataChannel('control', dataChannelInit);
      
      this.dataChannel.onopen = () => {
        console.log('DataChannel已打开');
      };
      
      this.dataChannel.onmessage = (event) => {
        this.handleDataChannelMessage(event.data);
      };
      
      this.dataChannel.onerror = (error) => {
        console.error('DataChannel错误:', error);
      };
    },
    
    /** 处理DataChannel消息 */
    handleDataChannelMessage(data) {
      try {
        const message = JSON.parse(data);
        console.log('收到DataChannel消息:', message);
        
        // 处理不同类型的消息
        switch (message.type) {
          case 'status':
            // 设备状态更新
            break;
          case 'error':
            this.$message.error('设备错误: ' + message.message);
            break;
        }
      } catch (e) {
        console.log('收到非JSON消息:', data);
      }
    },
    
    /** 发送信令消息 */
    sendSignaling(data) {
      if (!this.mqttClient || !this.mqttConnected) {
        console.error('MQTT未连接');
        return;
      }
      
      const config = JSON.parse(localStorage.getItem('mqttConnectionConfig'));
      const topic = `webrtc/${config.username}/${this.selectedDevice}`;
      
      this.mqttClient.publish(topic, JSON.stringify(data), { qos: 1 }, (err) => {
        if (err) {
          console.error('发送信令失败:', err);
        }
      });
    },
    
    /** 处理Offer */
    async handleOffer(offer) {
      await this.createPeerConnection();
      await this.peerConnection.setRemoteDescription(new RTCSessionDescription(offer));
      
      const answer = await this.peerConnection.createAnswer();
      await this.peerConnection.setLocalDescription(answer);
      
      this.sendSignaling({
        type: 'answer',
        deviceName: this.selectedDevice,
        answer: answer
      });
    },
    
    /** 处理Answer */
    async handleAnswer(answer) {
      await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
    },
    
    /** 处理ICE候选 */
    async handleIceCandidate(candidate) {
      if (this.peerConnection) {
        await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      }
    },
    
    /** 切换摄像头 */
    switchCamera() {
      if (!this.dataChannel || this.dataChannel.readyState !== 'open') {
        this.$message.warning('DataChannel未连接');
        return;
      }
      
      const message = JSON.stringify({
        action: 'switchCamera',
        cameraId: this.selectedCamera
      });
      
      this.dataChannel.send(message);
      this.$message.success('已发送切换摄像头指令');
    },
    
    /** 请求屏幕共享 */
    requestScreenShare() {
      if (!this.dataChannel || this.dataChannel.readyState !== 'open') {
        this.$message.warning('请先建立连接');
        return;
      }
      
      const message = JSON.stringify({
        action: 'startScreenShare'
      });
      
      this.dataChannel.send(message);
      this.$message.success('已发送屏幕共享请求');
    },
    
    /** 应用视频设置 */
    applyVideoSettings() {
      if (!this.dataChannel || this.dataChannel.readyState !== 'open') {
        return;
      }
      
      const [width, height] = this.videoConstraints.resolution.split('x');
      
      const message = JSON.stringify({
        action: 'updateVideoSettings',
        settings: {
          width: parseInt(width),
          height: parseInt(height),
          frameRate: this.videoConstraints.frameRate
        }
      });
      
      this.dataChannel.send(message);
      this.$message.success('视频设置已更新');
    },
    
    /** 全屏切换 */
    toggleFullscreen() {
      const videoWrapper = this.$refs.videoWrapper;
      
      if (!this.isFullscreen) {
        if (videoWrapper.requestFullscreen) {
          videoWrapper.requestFullscreen();
        } else if (videoWrapper.webkitRequestFullscreen) {
          videoWrapper.webkitRequestFullscreen();
        } else if (videoWrapper.mozRequestFullScreen) {
          videoWrapper.mozRequestFullScreen();
        }
        this.isFullscreen = true;
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen();
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen();
        } else if (document.mozCancelFullScreen) {
          document.mozCancelFullScreen();
        }
        this.isFullscreen = false;
      }
    },
    
    /** 截图 */
    captureScreenshot() {
      const video = this.$refs.remoteVideo;
      if (!video.srcObject) {
        this.$message.warning('当前没有视频流');
        return;
      }
      
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      
      const ctx = canvas.getContext('2d');
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      
      canvas.toBlob((blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `screenshot_${Date.now()}.png`;
        a.click();
        URL.revokeObjectURL(url);
        this.$message.success('截图已保存');
      });
    },
    
    /** 清理资源 */
    cleanup() {
      if (this.dataChannel) {
        this.dataChannel.close();
        this.dataChannel = null;
      }
      
      if (this.peerConnection) {
        this.peerConnection.close();
        this.peerConnection = null;
      }
      
      if (this.localStream) {
        this.localStream.getTracks().forEach(track => track.stop());
        this.localStream = null;
      }
      
      if (this.remoteStream) {
        this.remoteStream.getTracks().forEach(track => track.stop());
        this.remoteStream = null;
      }
      
      if (this.$refs.remoteVideo) {
        this.$refs.remoteVideo.srcObject = null;
      }
    }
  }
};
</script>

<style scoped>
.screencast-container {
  padding: 20px;
}

.control-panel {
  margin-bottom: 20px;
}

.device-selector {
  display: flex;
  align-items: center;
}

.connection-status {
  text-align: right;
  padding: 10px 0;
}

.video-area {
  margin-top: 20px;
}

.video-card {
  height: calc(100vh - 250px);
  min-height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.video-wrapper {
  position: relative;
  width: 100%;
  height: calc(100% - 60px);
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-wrapper video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-wrapper video.fullscreen {
  object-fit: cover;
}

.no-stream-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #909399;
}

.no-stream-placeholder i {
  font-size: 80px;
  margin-bottom: 20px;
}

.no-stream-placeholder p {
  font-size: 16px;
}

.control-card {
  height: calc(100vh - 250px);
  min-height: 500px;
  overflow-y: auto;
}

.control-section {
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #EBEEF5;
}

.control-section:last-child {
  border-bottom: none;
}

.control-section h4 {
  margin: 0 0 15px 0;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.control-buttons {
  width: 100%;
  display: flex;
}

.control-buttons .el-button {
  flex: 1;
}

.stats-info {
  font-size: 13px;
  color: #606266;
}

.stats-info p {
  margin: 8px 0;
  display: flex;
  justify-content: space-between;
}

.stats-info p span:first-child {
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .video-area .el-col {
    width: 100%;
    margin-bottom: 20px;
  }
  
  .video-card,
  .control-card {
    height: auto;
    min-height: 400px;
  }
}
</style>
