<template>
  <el-dialog
    :visible.sync="visible"
    :width="dialogWidth + 'px'"
    :fullscreen="isMobile"
    :show-close="false"
    :close-on-click-modal="false"
    custom-class="screencast-control-dialog"
    @close="handleClose"
    @open="handleOpen"
  >
    <div class="screencast-wrapper" :style="{ height: dialogHeight + 'px' }">
      <!-- 顶部工具栏 - 可拖动 -->
      <div class="top-toolbar" @mousedown="startDrag">
        <div class="device-name">{{ deviceName }}</div>
        <div class="toolbar-actions">
          <i class="el-icon-setting" @click.stop="showSettings = !showSettings"></i>
          <i class="el-icon-close" @click.stop="closeDialog"></i>
        </div>
      </div>

      <!-- 视频显示区 -->
      <div class="video-container" @click="toggleFullscreen">
        <video
          ref="remoteVideo"
          autoplay
          playsinline
          class="video-stream"
          controls
        ></video>
        <div v-if="!isStreaming" class="stream-placeholder">
          <i class="el-icon-loading" v-if="connectionStatus === 'connecting'"></i>
          <i class="el-icon-video-camera" v-else></i>
          <p>{{ statusText }}</p>
        </div>

        <!-- 右侧功能按钮 -->
        <div class="side-controls">
          <div class="control-item" @click.stop="sendVirtualKey('home')" :class="{ disabled: !isStreaming }">
            <i class="el-icon-s-home"></i>
            <span>主页</span>
          </div>
          <div class="control-item" @click.stop="sendVirtualKey('back')" :class="{ disabled: !isStreaming }">
            <i class="el-icon-back"></i>
            <span>返回</span>
          </div>
          <div class="control-item" @click.stop="captureScreenshot" :class="{ disabled: !isStreaming }">
            <i class="el-icon-picture"></i>
            <span>截图</span>
          </div>
          <div class="control-item" @click.stop="requestScreenShare">
            <i class="el-icon-refresh"></i>
            <span>刷新</span>
          </div>
        </div>
      </div>

      <!-- 设置面板 -->
      <div class="settings-panel" v-show="showSettings" @click.stop>
        <div class="setting-item">
          <span>分辨率</span>
          <el-select v-model="videoConstraints.resolution" size="mini" @change="applyVideoSettings">
            <el-option label="480x320" value="480x320" />
            <el-option label="640x480" value="640x480" />
            <el-option label="1280x720" value="1280x720" />
          </el-select>
        </div>
        <div class="setting-item">
          <span>帧率</span>
          <el-select v-model="videoConstraints.frameRate" size="mini" @change="applyVideoSettings">
            <el-option label="15 fps" :value="15" />
            <el-option label="30 fps" :value="30" />
          </el-select>
        </div>
      </div>

      <!-- 缩放手柄 -->
      <div class="resize-handles" v-if="!isMobile">
        <div class="resize-handle resize-right" @mousedown="startResize($event, 'right')"></div>
        <div class="resize-handle resize-bottom" @mousedown="startResize($event, 'bottom')"></div>
        <div class="resize-handle resize-corner" @mousedown="startResize($event, 'corner')"></div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ScreencastDialog',
  props: {
    value: {
      type: Boolean,
      default: false
    },
    deviceName: {
      type: String,
      required: true
    },
    mqttClient: {
      type: Object,
      default: null
    },
    username: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      visible: this.value,
      connectionStatus: 'disconnected', // disconnected, connecting, connected
      statusText: '等待连接...',
      isStreaming: false,
      showSettings: false,
      
      // 拖动和缩放相关
      dialogWidth: 420,
      dialogHeight: 700,
      isDragging: false,
      isResizing: false,
      resizeType: '',
      dragStartX: 0,
      dragStartY: 0,
      dialogLeft: 0,
      dialogTop: 0,
      resizeStartX: 0,
      resizeStartY: 0,
      resizeStartWidth: 0,
      resizeStartHeight: 0,
      
      // WebRTC相关
      peerConnection: null,
      dataChannel: null,
      remoteStream: null,
      
      videoConstraints: {
        resolution: '640x480',
        frameRate: 30
      },
      
      isMobile: false,
      
      iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
        {
          urls: 'turn:openrelay.metered.ca:80',
          username: 'openrelayproject',
          credential: 'openrelayproject'
        },
        {
          urls: 'turn:openrelay.metered.ca:443',
          username: 'openrelayproject',
          credential: 'openrelayproject'
        }
      ]
    };
  },
  watch: {
    value(val) {
      this.visible = val;
    },
    visible(val) {
      this.$emit('input', val);
    }
  },
  created() {
    this.checkMobile();
    document.addEventListener('mousemove', this.handleMouseMove);
    document.addEventListener('mouseup', this.handleMouseUp);
  },
  beforeDestroy() {
    this.cleanup();
    document.removeEventListener('mousemove', this.handleMouseMove);
    document.removeEventListener('mouseup', this.handleMouseUp);
  },
  methods: {
    /** 检测移动设备 */
    checkMobile() {
      this.isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    },
    
    /** 弹窗打开时自动连接 */
    handleOpen() {
      console.log('=== 开始投屏 ===');
      console.log('设备名称:', this.deviceName);
      console.log('MQTT连接状态:', !!this.mqttClient);
      console.log('用户名:', this.username);
      
      this.statusText = '正在连接...';
      this.connectionStatus = 'connecting';
      this.startScreencast();
    },
    
    /** 关闭弹窗 */
    handleClose() {
      this.stopScreencast();
      this.cleanup();
    },
    
    /** 关闭对话框 */
    closeDialog() {
      this.visible = false;
    },
    
    /** 开始投屏 */
    async startScreencast() {
      if (!this.mqttClient) {
        this.$message.error('MQTT未连接');
        return;
      }

      try {
        this.connectionStatus = 'connecting';
        this.statusText = '正在建立连接...';

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
          deviceName: this.deviceName,
          offer: offer
        });

        this.$message.success('正在建立连接...');
      } catch (e) {
        console.error('开始投屏失败:', e);
        this.$message.error('开始投屏失败: ' + e.message);
        this.connectionStatus = 'disconnected';
        this.statusText = '连接失败';
      }
    },
    
    /** 停止投屏 */
    stopScreencast() {
      this.cleanup();
      this.connectionStatus = 'disconnected';
      this.isStreaming = false;
      this.statusText = '已停止';
    },
    
    /** 创建PeerConnection */
    async createPeerConnection() {
      const configuration = {
        iceServers: this.iceServers
      };

      this.peerConnection = new RTCPeerConnection(configuration);

      // 添加视频接收器
      console.log('>>> 添加视频接收器...');
      this.peerConnection.addTransceiver('video', { direction: 'recvonly' });
      console.log('✓ 视频接收器已添加');

      // 添加音频接收器
      console.log('>>> 添加音频接收器...');
      this.peerConnection.addTransceiver('audio', { direction: 'recvonly' });
      console.log('✓ 音频接收器已添加');

      // ICE候选事件
      this.peerConnection.onicecandidate = (event) => {
        if (event.candidate) {
          console.log('>>> 生成ICE候选，准备发送');
          this.sendSignaling({
            type: 'ice-candidate',
            deviceName: this.deviceName,
            candidate: event.candidate,
            from: 'frontend'
          });
        } else {
          console.log('>>> ICE候选收集完成');
        }
      };

      // 连接状态变化
      this.peerConnection.onconnectionstatechange = () => {
        console.log('>>> PeerConnection状态变化:', this.peerConnection.connectionState);
        
        if (this.peerConnection.connectionState === 'connected') {
          this.connectionStatus = 'connected';
          this.statusText = '连接成功';
          this.$message.success('连接成功');
          console.log('✓ WebRTC连接成功！');
        } else if (this.peerConnection.connectionState === 'failed' ||
                   this.peerConnection.connectionState === 'disconnected') {
          this.connectionStatus = 'disconnected';
          this.isStreaming = false;
          this.statusText = '连接断开';
          console.error('✗ WebRTC连接失败或断开');
        }
      };

      // ICE连接状态变化
      this.peerConnection.oniceconnectionstatechange = () => {
        console.log('>>> ICE连接状态变化:', this.peerConnection.iceConnectionState);
        
        if (this.peerConnection.iceConnectionState === 'connected' || 
            this.peerConnection.iceConnectionState === 'completed') {
          console.log('✓ ICE连接成功！');
        } else if (this.peerConnection.iceConnectionState === 'failed') {
          console.error('✗ ICE连接失败 - 可能是NAT/防火墙问题');
          this.$message.error('ICE连接失败，请检查网络配置');
        }
      };

      // 接收远程流
      this.peerConnection.ontrack = (event) => {
        console.log('!!! ✓ 接收到远程流事件触发 !!!');
        console.log('轨道类型:', event.track.kind);
        
        if (event.streams && event.streams.length > 0) {
          const audioTracks = event.streams[0].getAudioTracks();
          const videoTracks = event.streams[0].getVideoTracks();
          console.log(`音频轨道: ${audioTracks.length}, 视频轨道: ${videoTracks.length}`);
          
          this.remoteStream = event.streams[0];
          
          if (this.$refs.remoteVideo) {
            this.$refs.remoteVideo.srcObject = this.remoteStream;
            console.log('✓ 视频元素srcObject已设置');
            
            this.$refs.remoteVideo.play().then(() => {
              console.log('✓ 视频开始播放');
            }).catch(err => {
              console.error('✗ 视频播放失败:', err);
            });
          }
          
          this.isStreaming = true;
          this.statusText = '正在播放';
          
          if (audioTracks.length > 0 && videoTracks.length > 0) {
            this.$message.success('视频和音频流已连接');
          } else if (videoTracks.length > 0) {
            this.$message.warning('视频流已连接，但没有音频');
          }
        }
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

        switch (message.type) {
          case 'status':
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
      if (!this.mqttClient) {
        console.error('MQTT未连接');
        return;
      }

      const topic = `webrtc/${this.username}/${this.deviceName}`;
      
      console.log('>>> 发送信令:', data.type);
      console.log('>>> 主题:', topic);

      this.mqttClient.publish(topic, JSON.stringify(data), { qos: 1 }, (err) => {
        if (err) {
          console.error('发送信令失败:', err);
        } else {
          console.log('✓ 信令发送成功:', data.type);
        }
      });
    },
    
    /** 处理Answer */
    async handleAnswer(answer) {
      console.log('>>> 开始设置远程描述（Answer）');
      await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
      console.log('✓ 远程描述设置成功');
    },
    
    /** 处理ICE候选 */
    async handleIceCandidate(candidate) {
      console.log('>>> 添加远程ICE候选');
      if (this.peerConnection) {
        await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
        console.log('✓ ICE候选添加成功');
      }
    },
    
    /** 发送虚拟按键 */
    sendVirtualKey(key) {
      if (!this.dataChannel || this.dataChannel.readyState !== 'open') {
        this.$message.warning('连接未建立');
        return;
      }

      const message = JSON.stringify({
        action: 'virtualKey',
        key: key
      });

      this.dataChannel.send(message);
      
      const keyNames = {
        'home': '主页',
        'back': '返回'
      };
      this.$message.success(`已发送${keyNames[key] || key}按键`);
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
      if (!this.isStreaming) {
        return;
      }
      
      const video = this.$refs.remoteVideo;

      if (!document.fullscreenElement) {
        if (video.requestFullscreen) {
          video.requestFullscreen();
        } else if (video.webkitRequestFullscreen) {
          video.webkitRequestFullscreen();
        } else if (video.mozRequestFullScreen) {
          video.mozRequestFullScreen();
        }
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen();
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen();
        } else if (document.mozCancelFullScreen) {
          document.mozCancelFullScreen();
        }
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
        a.download = `screenshot_${this.deviceName}_${Date.now()}.png`;
        a.click();
        URL.revokeObjectURL(url);
        this.$message.success('截图已保存');
      });
    },
    
    /** 开始拖动 */
    startDrag(e) {
      if (this.isMobile) return;
      
      this.isDragging = true;
      this.dragStartX = e.clientX;
      this.dragStartY = e.clientY;
      
      const dialog = document.querySelector('.screencast-control-dialog');
      if (dialog) {
        const rect = dialog.getBoundingClientRect();
        this.dialogLeft = rect.left;
        this.dialogTop = rect.top;
      }
      
      e.preventDefault();
    },
    
    /** 开始缩放 */
    startResize(e, type) {
      if (this.isMobile) return;
      
      this.isResizing = true;
      this.resizeType = type;
      this.resizeStartX = e.clientX;
      this.resizeStartY = e.clientY;
      this.resizeStartWidth = this.dialogWidth;
      this.resizeStartHeight = this.dialogHeight;
      
      e.preventDefault();
      e.stopPropagation();
    },
    
    /** 鼠标移动 */
    handleMouseMove(e) {
      if (this.isDragging) {
        const deltaX = e.clientX - this.dragStartX;
        const deltaY = e.clientY - this.dragStartY;
        
        const dialog = document.querySelector('.screencast-control-dialog');
        if (dialog) {
          dialog.style.left = (this.dialogLeft + deltaX) + 'px';
          dialog.style.top = (this.dialogTop + deltaY) + 'px';
          dialog.style.margin = '0';
        }
      } else if (this.isResizing) {
        const deltaX = e.clientX - this.resizeStartX;
        const deltaY = e.clientY - this.resizeStartY;
        
        if (this.resizeType === 'right' || this.resizeType === 'corner') {
          this.dialogWidth = Math.max(300, this.resizeStartWidth + deltaX);
        }
        
        if (this.resizeType === 'bottom' || this.resizeType === 'corner') {
          this.dialogHeight = Math.max(400, this.resizeStartHeight + deltaY);
        }
      }
    },
    
    /** 鼠标释放 */
    handleMouseUp() {
      this.isDragging = false;
      this.isResizing = false;
      this.resizeType = '';
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

      if (this.remoteStream) {
        this.remoteStream.getTracks().forEach(track => track.stop());
        this.remoteStream = null;
      }

      if (this.$refs.remoteVideo) {
        this.$refs.remoteVideo.srcObject = null;
      }

      this.isStreaming = false;
    }
  }
};
</script>

<style scoped>
/* 投屏控制弹窗 - 简洁直角风格 */
.screencast-wrapper {
  position: relative;
  width: 100%;
  background: #000;
  display: flex;
  flex-direction: column;
}

/* 顶部工具栏 */
.top-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  position: relative;
  z-index: 10;
  cursor: move;
  user-select: none;
}

.device-name {
  font-size: 14px;
  font-weight: 500;
}

.toolbar-actions {
  display: flex;
  gap: 15px;
}

.toolbar-actions i {
  font-size: 18px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.toolbar-actions i:hover {
  opacity: 0.7;
}

/* 视频容器 */
.video-container {
  position: relative;
  flex: 1;
  background: #000;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-stream {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.stream-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #fff;
}

.stream-placeholder i {
  font-size: 50px;
  margin-bottom: 12px;
  display: block;
  opacity: 0.6;
}

.stream-placeholder p {
  font-size: 13px;
  margin: 0;
  opacity: 0.6;
}

/* 右侧控制按钮 */
.side-controls {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 20px;
  z-index: 10;
}

.control-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
  user-select: none;
}

.control-item:hover:not(.disabled) {
  background: rgba(0, 0, 0, 0.8);
}

.control-item.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.control-item i {
  font-size: 20px;
  margin-bottom: 2px;
}

.control-item span {
  font-size: 10px;
}

/* 设置面板 */
.settings-panel {
  position: absolute;
  top: 50px;
  right: 10px;
  background: rgba(0, 0, 0, 0.9);
  color: #fff;
  padding: 15px;
  min-width: 200px;
  z-index: 20;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-item span {
  font-size: 13px;
  margin-right: 10px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .screencast-wrapper {
    height: 100vh;
    max-height: none;
  }
  
  .top-toolbar {
    padding: 10px 12px;
  }
  
  .device-name {
    font-size: 13px;
  }
  
  .toolbar-actions i {
    font-size: 16px;
  }
  
  .side-controls {
    right: 8px;
    gap: 15px;
  }
  
  .control-item {
    width: 45px;
    height: 45px;
  }
  
  .control-item i {
    font-size: 18px;
  }
  
  .control-item span {
    font-size: 9px;
  }
  
  .settings-panel {
    top: 45px;
    right: 8px;
    padding: 12px;
    min-width: 180px;
  }
}

/* 弹窗样式 - 移除圆角 */
::v-deep .screencast-control-dialog {
  border-radius: 0;
}

::v-deep .screencast-control-dialog .el-dialog__header {
  display: none;
}

::v-deep .screencast-control-dialog .el-dialog__body {
  padding: 0;
  background: #000;
}

::v-deep .screencast-control-dialog .el-dialog__headerbtn {
  display: none;
}

/* 全屏模式 */
::v-deep .screencast-control-dialog.el-dialog--fullscreen {
  margin: 0;
}

::v-deep .screencast-control-dialog.el-dialog--fullscreen .screencast-wrapper {
  height: 100vh;
  max-height: none;
}

/* 设置面板中的select样式 */
::v-deep .settings-panel .el-select {
  width: 110px;
}

::v-deep .settings-panel .el-input__inner {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  color: #fff;
}

/* 缩放手柄 */
.resize-handles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.resize-handle {
  position: absolute;
  pointer-events: auto;
  z-index: 100;
}

.resize-right {
  right: 0;
  top: 0;
  width: 5px;
  height: 100%;
  cursor: ew-resize;
}

.resize-bottom {
  bottom: 0;
  left: 0;
  width: 100%;
  height: 5px;
  cursor: ns-resize;
}

.resize-corner {
  right: 0;
  bottom: 0;
  width: 15px;
  height: 15px;
  cursor: nwse-resize;
  background: rgba(255, 255, 255, 0.3);
}

.resize-corner::after {
  content: '';
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 0 10px 10px;
  border-color: transparent transparent rgba(255, 255, 255, 0.5) transparent;
}
</style>
