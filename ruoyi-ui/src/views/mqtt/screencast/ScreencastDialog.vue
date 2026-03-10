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
import WebRTCManager from '@/utils/webrtc/WebRTCManager';

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
      connectionStatus: 'disconnected',
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
      
      // WebRTC 管理器
      webrtcManager: null,
      
      videoConstraints: {
        resolution: '640x480',
        frameRate: 30
      },
      
      isMobile: false
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
    this.initWebRTC();
    document.addEventListener('mousemove', this.handleMouseMove);
    document.addEventListener('mouseup', this.handleMouseUp);
  },
  beforeDestroy() {
    this.cleanup();
    document.removeEventListener('mousemove', this.handleMouseMove);
    document.removeEventListener('mouseup', this.handleMouseUp);
  },
  methods: {
    /** 初始化 WebRTC 管理器 */
    initWebRTC() {
      this.webrtcManager = new WebRTCManager({
        deviceName: this.deviceName,
        mqttClient: this.mqttClient,
        username: this.username
      });
      
      // 设置事件回调
      this.webrtcManager.on('connectionStateChange', (state) => {
        this.connectionStatus = state;
        
        if (state === 'connected') {
          this.statusText = '连接成功';
          this.$message.success('连接成功');
        } else if (state === 'failed' || state === 'disconnected') {
          this.isStreaming = false;
          this.statusText = '连接断开';
        }
      });
      
      this.webrtcManager.on('track', (stream, { audioTracks, videoTracks }) => {
        console.log('设置视频元素srcObject...');
        if (this.$refs.remoteVideo) {
          this.$refs.remoteVideo.srcObject = stream;
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
      });
      
      this.webrtcManager.on('dataChannelMessage', (message) => {
        switch (message.type) {
          case 'status':
            break;
          case 'error':
            this.$message.error('设备错误: ' + message.message);
            break;
        }
      });
      
      this.webrtcManager.on('error', (error) => {
        this.$message.error(error);
      });
    },
    
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
      
      // 更新 WebRTC 管理器配置
      this.webrtcManager.updateConfig({
        deviceName: this.deviceName,
        mqttClient: this.mqttClient,
        username: this.username
      });
      
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
        
        await this.webrtcManager.start();
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
      if (this.webrtcManager) {
        this.webrtcManager.stop();
      }
      this.connectionStatus = 'disconnected';
      this.isStreaming = false;
      this.statusText = '已停止';
    },
    
    /** 处理 Answer */
    async handleAnswer(answer) {
      if (this.webrtcManager) {
        await this.webrtcManager.handleAnswer(answer);
      }
    },
    
    /** 处理 ICE 候选 */
    async handleIceCandidate(candidate) {
      if (this.webrtcManager) {
        await this.webrtcManager.handleIceCandidate(candidate);
      }
    },
    
    /** 发送虚拟按键 */
    sendVirtualKey(key) {
      if (!this.webrtcManager || !this.webrtcManager.isDataChannelOpen()) {
        this.$message.warning('连接未建立');
        return;
      }

      this.webrtcManager.sendVirtualKey(key);
      
      const keyNames = {
        'home': '主页',
        'back': '返回'
      };
      this.$message.success(`已发送${keyNames[key] || key}按键`);
    },
    
    /** 请求屏幕共享 */
    requestScreenShare() {
      if (!this.webrtcManager || !this.webrtcManager.isDataChannelOpen()) {
        this.$message.warning('请先建立连接');
        return;
      }

      this.webrtcManager.requestScreenShare();
      this.$message.success('已发送屏幕共享请求');
    },
    
    /** 应用视频设置 */
    applyVideoSettings() {
      if (!this.webrtcManager || !this.webrtcManager.isDataChannelOpen()) {
        return;
      }

      const [width, height] = this.videoConstraints.resolution.split('x');

      this.webrtcManager.updateVideoSettings({
        width: parseInt(width),
        height: parseInt(height),
        frameRate: this.videoConstraints.frameRate
      });
      
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
      if (this.webrtcManager) {
        this.webrtcManager.cleanup();
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
