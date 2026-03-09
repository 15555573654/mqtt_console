<template>
  <div class="app-container">
    <!-- 统计信息 -->
    <el-row :gutter="20" class="mb8">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="statistic-item">
            <i class="el-icon-monitor statistic-icon"></i>
            <div class="statistic-content">
              <div class="statistic-value">{{ statistics.totalDevices || 0 }}</div>
              <div class="statistic-label">设备总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="statistic-item">
            <i class="el-icon-success statistic-icon" style="color: #67C23A"></i>
            <div class="statistic-content">
              <div class="statistic-value">{{ statistics.onlineDevices || 0 }}</div>
              <div class="statistic-label">在线设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="statistic-item">
            <i class="el-icon-error statistic-icon" style="color: #F56C6C"></i>
            <div class="statistic-content">
              <div class="statistic-value">{{ statistics.offlineDevices || 0 }}</div>
              <div class="statistic-label">离线设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="statistic-item">
            <i class="el-icon-coin statistic-icon" style="color: #E6A23C"></i>
            <div class="statistic-content">
              <div class="statistic-value">{{ statistics.totalDiamonds || 0 }}</div>
              <div class="statistic-label">钻石总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 查询和连接表单 -->
    <div class="query-connection-wrapper">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px" class="query-form">
        <el-form-item label="设备名称" prop="deviceName">
          <el-input
            v-model="queryParams.deviceName"
            placeholder="请输入设备名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="设备状态" prop="deviceStatus">
          <el-select v-model="queryParams.deviceStatus" placeholder="设备状态" clearable>
            <el-option label="在线" value="在线" />
            <el-option label="离线" value="离线" />
          </el-select>
        </el-form-item>
        <el-form-item label="脚本状态" prop="scriptStatus">
          <el-select v-model="queryParams.scriptStatus" placeholder="脚本状态" clearable>
            <el-option label="运行中" value="运行中" />
            <el-option label="未运行" value="未运行" />
            <el-option label="暂停" value="暂停" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- MQTT连接设置 -->
      <el-form ref="connectionForm" :model="connectionForm" :rules="connectionRules" inline size="small" class="connection-form">
        <el-form-item label="服务器状态">
          <el-tag :type="isConnected ? 'success' : 'info'" size="medium">
            {{ isConnected ? '已连接' : '未连接' }}
          </el-tag>
          <span v-if="isConnected" style="margin-left: 10px; color: #67C23A; font-size: 12px">
            <i class="el-icon-user"></i> {{ currentUsername }}
          </span>
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="connectionForm.username" placeholder="用户名" :disabled="isConnected" style="width: 120px" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="connectionForm.password" type="password" placeholder="密码" show-password :disabled="isConnected" style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button
            v-if="!isConnected"
            type="primary"
            size="small"
            @click="handleConnect"
            :loading="connecting"
            v-hasPermi="['mqtt:connection:connect']"
          >连接</el-button>
          <el-button
            v-else
            type="danger"
            size="small"
            @click="handleDisconnect"
            v-hasPermi="['mqtt:connection:disconnect']"
          >断开</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-video-play"
          size="mini"
          :disabled="multiple || !isConnected"
          @click="handleCommand('start')"
          v-hasPermi="['mqtt:device:command']"
        >启动</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-video-pause"
          size="mini"
          :disabled="multiple || !isConnected"
          @click="handleCommand('stop')"
          v-hasPermi="['mqtt:device:command']"
        >停止</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-video-pause"
          size="mini"
          :disabled="multiple || !isConnected"
          @click="handleCommand('pause')"
          v-hasPermi="['mqtt:device:command']"
        >暂停</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-video-play"
          size="mini"
          :disabled="multiple || !isConnected"
          @click="handleCommand('resume')"
          v-hasPermi="['mqtt:device:command']"
        >恢复</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-refresh"
          size="mini"
          :disabled="multiple || !isConnected"
          @click="handleCommand('updateScript')"
          v-hasPermi="['mqtt:device:command']"
        >更新脚本</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple || !isConnected"
          @click="handleDelete"
          v-hasPermi="['mqtt:device:remove']"
        >删除设备</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-video-camera"
          size="mini"
          :disabled="single || !isConnected"
          @click="openScreencastDialog"
          v-hasPermi="['mqtt:device:screencast']"
        >投屏控制</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="refreshData"></right-toolbar>
    </el-row>

    <!-- 设备列表 -->
    <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" type="index" width="50" align="center" />
      <el-table-column label="设备名称" align="center" prop="deviceName" :show-overflow-tooltip="true" />
      <el-table-column label="设备状态" align="center" prop="deviceStatus" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.deviceStatus === '在线' ? 'success' : 'info'">
            {{ scope.row.deviceStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="脚本状态" align="center" prop="scriptStatus" width="100">
        <template slot-scope="scope">
          <el-tag
            :type="scope.row.scriptStatus === '运行中' ? 'success' : (scope.row.scriptStatus === '暂停' ? 'warning' : 'info')">
            {{ scope.row.scriptStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="等级" align="center" prop="level" width="80" />
      <el-table-column label="区服" align="center" prop="server" :show-overflow-tooltip="true" />
      <el-table-column label="钻石数量" align="center" prop="diamonds" width="100" />
      <el-table-column label="任务配置" align="center" prop="taskConfig" :show-overflow-tooltip="true" />
      <el-table-column label="最后在线" align="center" prop="lastOnline" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.lastOnline) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 投屏控制弹窗 -->
    <el-dialog
      :visible.sync="screencastDialogVisible"
      :width="dialogWidth + 'px'"
      :fullscreen="isMobile"
      :show-close="false"
      :close-on-click-modal="false"
      custom-class="screencast-control-dialog"
      @close="handleCloseScreencast"
      @open="handleOpenScreencast"
    >
      <div class="screencast-wrapper" :style="{ height: dialogHeight + 'px' }">
        <!-- 顶部工具栏 - 可拖动 -->
        <div class="top-toolbar" @mousedown="startDrag">
          <div class="device-name">{{ screencastDevice }}</div>
          <div class="toolbar-actions">
            <i class="el-icon-setting" @click.stop="showSettings = !showSettings"></i>
            <i class="el-icon-close" @click.stop="screencastDialogVisible = false"></i>
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
            <i class="el-icon-loading" v-if="screencastConnectionStatus === 'connecting'"></i>
            <i class="el-icon-video-camera" v-else></i>
            <p>{{ screencastStatus }}</p>
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
        <div class="resize-handles">
          <div class="resize-handle resize-right" @mousedown="startResize($event, 'right')"></div>
          <div class="resize-handle resize-bottom" @mousedown="startResize($event, 'bottom')"></div>
          <div class="resize-handle resize-corner" @mousedown="startResize($event, 'corner')"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDevice, delDevice, sendCommand, getStatistics, batchSaveDevices } from "@/api/mqtt/device";
import { parseTime } from "@/utils/ruoyi";
import mqtt from 'mqtt';

export default {
  name: "MqttDevice",
  computed: {
    screencastConnectionStatusText() {
      const statusMap = {
        'disconnected': '未连接',
        'connecting': '连接中...',
        'connected': '已连接'
      };
      return statusMap[this.screencastConnectionStatus] || '未知';
    }
  },
  data() {
    return {
      // MQTT连接相关
      mqttClient: null,
      isConnected: false,
      currentUsername: '',
      connecting: false,
      connectionForm: {
        mqttHost: '192.168.1.132',
        mqttPort: '8083',
        username: '',
        password: ''
      },
      connectionRules: {
        username: [
          { required: true, message: "用户名不能为空", trigger: "blur" }
        ],
        password: [
          { required: true, message: "密码不能为空", trigger: "blur" }
        ]
      },
      // 实时设备数据（从MQTT接收）
      realtimeDevices: {},
      // 定时保存定时器
      saveTimer: null,
      // 数据是否有变化
      dataChanged: false,
      // 投屏控制相关
      screencastDialogVisible: false,
      screencastDevice: '',
      screencastConnectionStatus: 'disconnected', // disconnected, connecting, connected
      screencastStatus: '等待连接...',
      isStreaming: false,
      screenViewDialogVisible: false,
      commandLoading: false,
      deviceInfo: null,
      showSettings: false,
      // 拖动和缩放相关
      dialogWidth: 420,
      dialogHeight: 700,
      isDragging: false,
      isResizing: false,
      resizeType: '',
      dragStartX: 0,
      dragStartY: 0,
      resizeStartX: 0,
      resizeStartY: 0,
      resizeStartWidth: 0,
      resizeStartHeight: 0,
      peerConnection: null,
      dataChannel: null,
      remoteStream: null,
      videoConstraints: {
        resolution: '640x480',
        frameRate: 30
      },
      videoStats: '',
      activeCollapse: [],
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
      ],
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 选中设备名称数组
      deviceNames: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 设备表格数据
      deviceList: [],
      // 统计信息
      statistics: {
        totalDevices: 0,
        onlineDevices: 0,
        offlineDevices: 0,
        totalDiamonds: 0
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deviceName: null,
        deviceStatus: null,
        scriptStatus: null
      }
    };
  },
  created() {
    // 从localStorage加载连接配置（只加载用户名和密码）
    this.loadConnectionConfig();
    // 加载历史设备数据
    this.getList();
    this.getStatistics();
    // 检测是否为移动设备
    this.checkMobile();
    // 添加全局鼠标事件监听
    document.addEventListener('mousemove', this.handleMouseMove);
    document.addEventListener('mouseup', this.handleMouseUp);
  },
  beforeDestroy() {
    // 断开MQTT连接
    if (this.mqttClient) {
      this.mqttClient.end();
    }
    // 清除定时器
    if (this.saveTimer) {
      clearInterval(this.saveTimer);
    }
    // 清理投屏资源
    this.cleanupScreencast();
    // 移除全局鼠标事件监听
    document.removeEventListener('mousemove', this.handleMouseMove);
    document.removeEventListener('mouseup', this.handleMouseUp);
  },
  methods: {
    /** 加载连接配置（只加载用户名和密码） */
    loadConnectionConfig() {
      const config = localStorage.getItem('mqttConnectionConfig');
      if (config) {
        try {
          const savedConfig = JSON.parse(config);
          // 只恢复用户名和密码，不恢复服务器IP和端口
          if (savedConfig.username) {
            this.connectionForm.username = savedConfig.username;
          }
          if (savedConfig.password) {
            this.connectionForm.password = savedConfig.password;
          }
        } catch (e) {
          console.error('加载连接配置失败', e);
        }
      }
    },
    /** 保存连接配置（只保存用户名和密码） */
    saveConnectionConfig() {
      const config = {
        username: this.connectionForm.username,
        password: this.connectionForm.password
      };
      localStorage.setItem('mqttConnectionConfig', JSON.stringify(config));
    },
    /** 连接MQTT */
    handleConnect() {
      this.$refs["connectionForm"].validate(valid => {
        if (valid) {
          this.connecting = true;

          const { mqttHost, mqttPort, username, password } = this.connectionForm;
          const url = `ws://${mqttHost}:${mqttPort}/mqtt`;

          const options = {
            clientId: 'mqtt_web_' + username + '_' + Math.random().toString(16).substr(2, 8),
            username: username,
            password: password,
            clean: true,
            reconnectPeriod: 0, // 禁用自动重连，避免一直尝试
            connectTimeout: 10000
          };

          try {
            this.mqttClient = mqtt.connect(url, options);

            // 连接成功
            this.mqttClient.on('connect', () => {
              this.isConnected = true;
              this.currentUsername = username;
              this.connecting = false;
              this.saveConnectionConfig();
              this.$modal.msgSuccess("MQTT连接成功");

              // 订阅主题
              this.subscribeTopics(username);

              // 启动定时保存（每30秒保存一次）
              this.startAutoSave();

              // 初始加载历史数据
              this.getList();
            });

            // 连接失败
            this.mqttClient.on('error', (err) => {
              console.error('MQTT连接错误:', err);
              this.connecting = false;
              this.isConnected = false;

              let errorMsg = "MQTT连接失败";
              if (err.message) {
                errorMsg += ": " + err.message;
              }

              // 常见错误提示
              if (err.message && err.message.includes('ECONNREFUSED')) {
                errorMsg = `连接被拒绝，请检查MQTT服务器地址和端口是否正确 (${mqttHost}:${mqttPort})`;
              } else if (err.message && err.message.includes('timeout')) {
                errorMsg = `连接超时，请检查MQTT服务器是否开启WebSocket支持 (${mqttHost}:${mqttPort})`;
              }

              this.$modal.msgError(errorMsg);

              // 清理客户端
              if (this.mqttClient) {
                this.mqttClient.end(true);
                this.mqttClient = null;
              }
            });

            // 连接断开
            this.mqttClient.on('close', () => {
              this.isConnected = false;
            });

            // 离线事件
            this.mqttClient.on('offline', () => {
              this.connecting = false;
              this.isConnected = false;
            });

            // 接收消息
            this.mqttClient.on('message', (topic, message) => {
              this.handleMqttMessage(topic, message.toString());
            });

            // 设置超时，如果10秒内没有连接成功，则取消
            setTimeout(() => {
              if (this.connecting) {
                this.connecting = false;
                this.$modal.msgError("连接超时，请检查MQTT服务器配置");
                if (this.mqttClient) {
                  this.mqttClient.end(true);
                  this.mqttClient = null;
                }
              }
            }, 10000);

          } catch (err) {
            console.error('创建MQTT客户端失败:', err);
            this.connecting = false;
            this.$modal.msgError("创建MQTT客户端失败: " + err.message);
          }
        }
      });
    },
    /** 统一脚本状态 */
    normalizeScriptStatus(rawStatus) {
      const statusMap = {
        '停止': '未运行',
        '运行中': '运行中',
        '暂停': '暂停',
        '在线': '运行中',
        'stopped': '未运行',
        'running': '运行中',
        'paused': '暂停'
      };
      return statusMap[rawStatus] || rawStatus;
    },
    /** 获取脚本状态字段（兼容中英文） */
    getRawScriptStatus(data) {
      return data.scriptStatus !== undefined ? data.scriptStatus : data.运行状态;
    },
    /** 订阅主题 */
    subscribeTopics(username) {
      if (!this.mqttClient) return;

      const topics = [
        `response/${username}/#`,
        `status/${username}/#`,
        `config/${username}/#`,
        `webrtc/${username}/#`
      ];

      console.log('=== 开始订阅MQTT主题 ===');
      topics.forEach(topic => {
        console.log('订阅主题:', topic);
        this.mqttClient.subscribe(topic, { qos: 1 }, (err) => {
          if (err) {
            console.error('✗ 订阅主题失败:', topic, err);
          } else {
            console.log('✓ 订阅主题成功:', topic);
          }
        });
      });
      console.log('=== 主题订阅完成 ===');
    },
    /** 处理WebRTC信令 */
    async handleWebRTCSignaling(data) {
      const { type, deviceName } = data;
      
      console.log('<<< 收到WebRTC信令 ===');
      console.log('<<< 信令类型:', type);
      console.log('<<< 设备名称:', deviceName);
      console.log('<<< 当前设备:', this.screencastDevice);
      console.log('<<< 完整数据:', JSON.stringify(data).substring(0, 200) + '...');

      if (deviceName !== this.screencastDevice) {
        console.warn('⚠ 设备名称不匹配，忽略信令');
        console.warn('期望:', this.screencastDevice, '实际:', deviceName);
        return;
      }

      try {
        switch (type) {
          case 'offer':
            // 前端是发起方，不应该处理Offer（忽略自己发送的）
            console.log('>>> 收到Offer，但前端是发起方，忽略');
            break;
          case 'answer':
            console.log('>>> 处理Answer...');
            await this.handleAnswer(data.answer);
            break;
          case 'ice-candidate':
            // 只处理来自设备端的ICE候选
            if (!data.from || data.from === 'device') {
              console.log('>>> 处理来自设备端的ICE候选...');
              await this.handleIceCandidate(data.candidate);
            } else {
              console.log('>>> 忽略自己发送的ICE候选');
            }
            break;
          case 'test':
            console.log('✓ 收到测试消息:', data.message);
            this.$message.success('收到设备端测试消息: ' + data.message);
            break;
          default:
            console.warn('未知的信令类型:', type);
        }
        console.log('✓ 信令处理成功:', type);
      } catch (e) {
        console.error('✗ 处理信令失败:', e);
        console.error('错误堆栈:', e.stack);
      }
    },
    /** 处理Offer */
    async handleOffer(offer) {
      await this.createPeerConnection();
      await this.peerConnection.setRemoteDescription(new RTCSessionDescription(offer));

      const answer = await this.peerConnection.createAnswer();
      await this.peerConnection.setLocalDescription(answer);

      this.sendSignaling({
        type: 'answer',
        deviceName: this.screencastDevice,
        answer: answer
      });
    },
    /** 处理Answer */
    async handleAnswer(answer) {
      console.log('>>> 开始设置远程描述（Answer）');
      console.log('>>> Answer SDP长度:', answer.sdp ? answer.sdp.length : 0);
      
      await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
      
      console.log('✓ 远程描述设置成功');
      console.log('>>> 当前信令状态:', this.peerConnection.signalingState);
    },
    /** 处理ICE候选 */
    async handleIceCandidate(candidate) {
      console.log('>>> 添加远程ICE候选');
      console.log('>>> 候选类型:', candidate.candidate ? candidate.candidate.split(' ')[7] : 'unknown');
      
      if (this.peerConnection) {
        await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
        console.log('✓ ICE候选添加成功');
      } else {
        console.error('✗ PeerConnection不存在，无法添加ICE候选');
      }
    },
    /** 处理MQTT消息 */
    handleMqttMessage(topic, payload) {
      try {
        console.log('=== 收到MQTT消息 ===');
        console.log('主题:', topic);
        console.log('消息长度:', payload.length);
        
        const data = JSON.parse(payload);
        
        console.log('消息类型:', data.type);
        console.log('设备名称:', data.deviceName);
        
        // 处理WebRTC信令消息
        if (topic.includes('webrtc/')) {
          console.log('>>> 这是WebRTC信令消息，转发处理');
          this.handleWebRTCSignaling(data);
          return;
        }
        
        const deviceName = data.deviceName;

        if (!deviceName) {
          console.warn('消息中没有deviceName:', payload);
          return;
        }

        // 初始化设备数据
        if (!this.realtimeDevices[deviceName]) {
          const newDevice = {
            deviceName: deviceName,
            username: this.currentUsername,
            deviceStatus: '离线',
            scriptStatus: '未运行',
            level: '',
            server: '',
            diamonds: '',
            taskConfig: '',
            lastOnline: new Date()
          };
          this.$set(this.realtimeDevices, deviceName, newDevice);
        }

        const device = this.realtimeDevices[deviceName];
        // 处理状态消息
        // 实际主题格式为: status/{username}/{deviceName}
        if (topic.startsWith('status/')) {
          // 设备在线/离线状态
          if (data.status) {
            this.$set(device, 'deviceStatus', data.status === 'online' ? '在线' : '离线');
            this.$set(device, 'lastOnline', new Date());
            this.dataChanged = true;
          }
          // 同时尝试从状态消息里更新脚本状态（如果客户端在这里返回）
          const rawScriptStatus = this.getRawScriptStatus(data);
          if (rawScriptStatus !== undefined) {
            this.$set(device, 'scriptStatus', this.normalizeScriptStatus(rawScriptStatus));
            this.dataChanged = true;
          }
        }
        // 处理响应消息（命令执行结果）
        // 实际主题格式为: response/{username}/{deviceName}
        else if (topic.startsWith('response/')) {
          const rawScriptStatus = this.getRawScriptStatus(data);
          if (rawScriptStatus !== undefined) {
            this.$set(device, 'scriptStatus', this.normalizeScriptStatus(rawScriptStatus));
            this.dataChanged = true;
          }
        }
        // 处理配置消息
        // 实际主题格式为: config/{username}/{deviceName}
        else if (topic.startsWith('config/')) {
          this.$set(device, 'deviceStatus', '在线');
          this.$set(device, 'lastOnline', new Date());

          if (data.type === 'post' && data.msg) {
            // msg字段是JSON字符串，需要再次解析
            try {
              const msgData = JSON.parse(data.msg);
              // 兼容旧版(中文字段)和新版(英文字段)的键名
              const levelVal = msgData.等级 !== undefined ? msgData.等级 : msgData.level;
              const serverVal = msgData.区服 !== undefined ? msgData.区服 : msgData.server;
              const diamondsVal = msgData.钻石 !== undefined ? msgData.钻石 : msgData.diamonds;
              const statusRaw = msgData.运行状态 !== undefined ? msgData.运行状态 : msgData.scriptStatus;

              if (levelVal !== undefined) {
                this.$set(device, 'level', levelVal);
              }
              if (serverVal !== undefined) {
                this.$set(device, 'server', serverVal);
              }
              if (diamondsVal !== undefined) {
                this.$set(device, 'diamonds', diamondsVal);
              }
              if (statusRaw !== undefined) {
                this.$set(device, 'scriptStatus', this.normalizeScriptStatus(statusRaw));
              }

              this.dataChanged = true;
            } catch (e) {
              console.error('解析msg字段失败:', e, data.msg);
            }
          }
        }

        // 更新显示列表和统计
        this.updateDeviceList();
        this.updateStatistics();

      } catch (e) {
        console.error('解析MQTT消息失败:', e, payload);
      }
    },
    /** 更新设备列表显示 */
    updateDeviceList() {
      const devices = Object.values(this.realtimeDevices);
      // 应用筛选条件
      let filteredDevices = devices;

      if (this.queryParams.deviceName) {
        filteredDevices = filteredDevices.filter(d =>
          d.deviceName.includes(this.queryParams.deviceName)
        );
      }

      if (this.queryParams.deviceStatus) {
        filteredDevices = filteredDevices.filter(d =>
          d.deviceStatus === this.queryParams.deviceStatus
        );
      }

      if (this.queryParams.scriptStatus) {
        filteredDevices = filteredDevices.filter(d =>
          d.scriptStatus === this.queryParams.scriptStatus
        );
      }

      // 分页
      this.total = filteredDevices.length;
      const start = (this.queryParams.pageNum - 1) * this.queryParams.pageSize;
      const end = start + this.queryParams.pageSize;

      // 使用Vue.set确保响应式更新
      this.deviceList = [...filteredDevices.slice(start, end)];
      this.loading = false;

    },
    /** 更新统计信息 */
    updateStatistics() {
      const devices = Object.values(this.realtimeDevices);

      this.statistics.totalDevices = devices.length;
      this.statistics.onlineDevices = devices.filter(d => d.deviceStatus === '在线').length;
      this.statistics.offlineDevices = devices.filter(d => d.deviceStatus === '离线').length;

      // 计算钻石总数
      let totalDiamonds = 0;
      devices.forEach(d => {
        const diamonds = parseInt(d.diamonds);
        if (!isNaN(diamonds)) {
          totalDiamonds += diamonds;
        }
      });
      this.statistics.totalDiamonds = totalDiamonds;
    },
    /** 启动自动保存 */
    startAutoSave() {
      if (this.saveTimer) {
        clearInterval(this.saveTimer);
      }
      // 每30秒保存一次到数据库
      this.saveTimer = setInterval(() => {
        if (this.dataChanged) {
          this.saveToDatabase();
        }
      }, 30000);
    },
    /** 保存到数据库 */
    saveToDatabase() {
      if (!this.dataChanged) return;

      const devices = Object.values(this.realtimeDevices);
      if (devices.length === 0) return;

      // 将 lastOnline 转成后端需要的字符串格式 yyyy-MM-dd HH:mm:ss
      const payloadDevices = devices.map(d => {
          const device = { ...d };
          if (device.lastOnline) {
            try {
              device.lastOnline = parseTime(device.lastOnline, '{y}-{m}-{d} {h}:{i}:{s}');
            } catch (e) {
              console.error('格式化 lastOnline 失败:', e, device.lastOnline);
              device.lastOnline = null;
            }
          } else {
            device.lastOnline = null;
          }
          return device;
        });

      // 调用后端API批量保存
      batchSaveDevices(payloadDevices).then(() => {
        this.dataChanged = false;
      }).catch(error => {
        console.error('设备数据保存失败:', error);
      });
    },
    /** 发送MQTT命令 */
    publishCommand(deviceName, action, params = {}) {
      if (!this.mqttClient || !this.isConnected) {
        this.$modal.msgError("MQTT未连接");
        return Promise.reject('MQTT未连接');
      }

      const topic = `commands/${this.currentUsername}/${deviceName}`;
      const message = JSON.stringify({
        action: action,
        ...params
      });

      return new Promise((resolve, reject) => {
        this.mqttClient.publish(topic, message, { qos: 1 }, (err) => {
          if (err) {
            console.error('发送命令失败:', err);
            reject(err);
          } else {
            console.log('发送命令成功:', topic, message);
            resolve();
          }
        });
      });
    },
    /** 断开连接 */
    handleDisconnect() {
      this.$modal.confirm('是否确认断开MQTT连接？').then(() => {
        // 先保存数据
        if (this.dataChanged) {
          this.saveToDatabase();
        }

        // 清除定时器
        if (this.saveTimer) {
          clearInterval(this.saveTimer);
          this.saveTimer = null;
        }

        if (this.mqttClient) {
          this.mqttClient.end();
          this.mqttClient = null;
        }
        this.isConnected = false;
        this.currentUsername = '';
        this.$modal.msgSuccess("MQTT连接已断开");
      }).catch(() => {});
    },
    /** 查询设备列表 */
    getList() {
      // 如果已连接MQTT，使用实时数据
      if (this.isConnected) {
        this.updateDeviceList();
        return;
      }

      // 未连接时，从数据库加载历史数据
      this.loading = true;
      listDevice(this.queryParams).then(response => {
        this.deviceList = response.rows;
        this.total = response.total;
        this.loading = false;

        // 将历史数据加载到realtimeDevices
        response.rows.forEach(device => {
          this.$set(this.realtimeDevices, device.deviceName, { ...device });
        });
      });
    },
    /** 获取统计信息 */
    getStatistics() {
      // 如果已连接MQTT，使用实时统计
      if (this.isConnected) {
        this.updateStatistics();
        return;
      }

      // 未连接时，从数据库获取统计
      getStatistics().then(response => {
        this.statistics = response.data;
      });
    },
    /** 刷新数据（列表+统计+主动请求状态） */
    refreshData() {
      this.getList();
      this.getStatistics();

      // 如果已连接MQTT，刷新时主动请求当前页设备的最新状态
      if (this.isConnected) {
        const deviceNames = this.deviceList.map(d => d.deviceName);
        if (deviceNames.length > 0) {
          const statusPromises = deviceNames.map(name => this.requestDeviceStatus(name));
          Promise.all(statusPromises).catch(err => {
            console.error('刷新时请求设备状态失败:', err);
          });
        }
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
      this.getStatistics();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.deviceId);
      this.deviceNames = selection.map(item => item.deviceName);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    /** 主动请求设备状态（通过MQTT命令） */
    requestDeviceStatus(deviceName) {
      // 约定使用 action = 'status' 主动查询设备当前状态
      return this.publishCommand(deviceName, 'status');
    },
    /** 发送命令 */
    handleCommand(action) {
      const actionNames = {
        'start': '启动',
        'stop': '停止',
        'pause': '暂停',
        'resume': '恢复',
        'updateScript': '更新脚本'
      };

      this.$modal.confirm('是否确认' + actionNames[action] + '选中的设备？').then(() => {
        // 使用前端MQTT直接发送命令
        const promises = this.deviceNames.map(deviceName => {
          return this.publishCommand(deviceName, action);
        });

        Promise.all(promises)
          .then(() => {
            // 命令发送成功后，主动再请求一次设备状态
            const statusPromises = this.deviceNames.map(deviceName => {
              return this.requestDeviceStatus(deviceName);
            });
            return Promise.all(statusPromises);
          })
          .then(() => {
            this.$modal.msgSuccess(actionNames[action] + "命令已发送，正在获取最新状态");
          })
          .catch(() => {
          this.$modal.msgError(actionNames[action] + "命令发送失败");
          });
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const deviceIds = row.deviceId || this.ids;
      this.$modal.confirm('是否确认删除选中的设备？').then(() => {
        return delDevice(deviceIds);
      }).then(() => {
        this.getList();
        this.getStatistics();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 投屏控制 */
    handleScreencast() {
      if (this.deviceNames.length === 1) {
        // 跳转到投屏控制页面，并传递设备名称
        this.$router.push({
          name: 'Screencast',
          query: { device: this.deviceNames[0] }
        });
      } else {
        this.$message.warning('请选择一个设备进行投屏');
      }
    },
    /** 检测移动设备 */
    checkMobile() {
      this.isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
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
    /** 打开投屏弹窗 */
    openScreencastDialog() {
      if (this.deviceNames.length === 1) {
        this.screencastDevice = this.deviceNames[0];
        this.screencastDialogVisible = true;
      } else {
        this.$message.warning('请选择一个设备进行投屏');
      }
    },
    /** 弹窗打开时自动连接 */
    handleOpenScreencast() {
      console.log('=== 开始投屏 ===');
      console.log('设备名称:', this.screencastDevice);
      console.log('MQTT连接状态:', this.isConnected);
      console.log('用户名:', this.currentUsername);
      
      this.screencastStatus = '正在连接...';
      this.screencastConnectionStatus = 'connecting';
      // 自动开始WebRTC连接
      this.startScreencast();
    },
    /** 关闭投屏弹窗 */
    handleCloseScreencast() {
      this.stopScreencast();
      this.cleanupScreencast();
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
    /** 发送控制命令 */
    async sendControlCommand(action) {
      if (!this.mqttClient || !this.isConnected) {
        this.$message.error('MQTT未连接');
        return;
      }

      this.commandLoading = true;

      const topic = `commands/${this.currentUsername}/${this.screencastDevice}`;
      const message = JSON.stringify({ action });

      this.mqttClient.publish(topic, message, { qos: 1 }, (err) => {
        this.commandLoading = false;
        if (err) {
          this.$message.error('命令发送失败');
        } else {
          const actionNames = {
            'start': '启动',
            'stop': '停止',
            'pause': '暂停',
            'resume': '恢复',
            'updateScript': '更新脚本'
          };
          this.$message.success(`${actionNames[action] || action}命令已发送`);
        }
      });
    },
    /** 关闭投屏弹窗 */
    handleCloseScreencast() {
      this.stopScreencast();
      this.cleanupScreencast();
    },
    /** 开始投屏 */
    async startScreencast() {
      if (!this.mqttClient || !this.isConnected) {
        this.$message.error('MQTT未连接');
        return;
      }

      try {
        this.screencastConnectionStatus = 'connecting';
        this.screencastStatus = '正在建立连接...';

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
          deviceName: this.screencastDevice,
          offer: offer
        });

        this.$message.success('正在建立连接...');
      } catch (e) {
        console.error('开始投屏失败:', e);
        this.$message.error('开始投屏失败: ' + e.message);
        this.screencastConnectionStatus = 'disconnected';
        this.screencastStatus = '连接失败';
      }
    },
    /** 停止投屏 */
    stopScreencast() {
      this.cleanupScreencast();
      this.screencastConnectionStatus = 'disconnected';
      this.isStreaming = false;
      this.screencastStatus = '已停止';
    },
    /** 创建PeerConnection */
    async createPeerConnection() {
      const configuration = {
        iceServers: this.iceServers
      };

      this.peerConnection = new RTCPeerConnection(configuration);

      // 添加视频接收器（重要！告诉对方我们想接收视频）
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
            deviceName: this.screencastDevice,
            candidate: event.candidate,
            from: 'frontend'  // 标识来自前端
          });
        } else {
          console.log('>>> ICE候选收集完成');
        }
      };

      // 连接状态变化
      this.peerConnection.onconnectionstatechange = () => {
        console.log('>>> PeerConnection状态变化:', this.peerConnection.connectionState);
        console.log('>>> ICE连接状态:', this.peerConnection.iceConnectionState);
        console.log('>>> ICE收集状态:', this.peerConnection.iceGatheringState);
        
        if (this.peerConnection.connectionState === 'connected') {
          this.screencastConnectionStatus = 'connected';
          this.screencastStatus = '连接成功';
          this.$message.success('连接成功');
          console.log('✓ WebRTC连接成功！');
        } else if (this.peerConnection.connectionState === 'failed' ||
                   this.peerConnection.connectionState === 'disconnected') {
          this.screencastConnectionStatus = 'disconnected';
          this.isStreaming = false;
          this.screencastStatus = '连接断开';
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
        } else if (this.peerConnection.iceConnectionState === 'disconnected') {
          console.warn('⚠ ICE连接断开');
        }
      };

      // 接收远程流
      this.peerConnection.ontrack = (event) => {
        console.log('!!! ✓ 接收到远程流事件触发 !!!');
        console.log('事件对象:', event);
        console.log('轨道类型:', event.track.kind);
        console.log('流数量:', event.streams.length);
        
        if (event.streams && event.streams.length > 0) {
          console.log('流ID:', event.streams[0].id);
          console.log('轨道数量:', event.streams[0].getTracks().length);
          
          const tracks = event.streams[0].getTracks();
          console.log('轨道详情:');
          tracks.forEach(track => {
            console.log(`  - ${track.kind}: ${track.label} (${track.readyState}, enabled: ${track.enabled})`);
          });
          
          // 检查音频轨道
          const audioTracks = event.streams[0].getAudioTracks();
          const videoTracks = event.streams[0].getVideoTracks();
          console.log(`音频轨道: ${audioTracks.length}, 视频轨道: ${videoTracks.length}`);
          
          this.remoteStream = event.streams[0];
          
          console.log('设置视频元素srcObject...');
          if (this.$refs.remoteVideo) {
            this.$refs.remoteVideo.srcObject = this.remoteStream;
            console.log('✓ 视频元素srcObject已设置');
            
            // 确保视频播放（包括音频）
            this.$refs.remoteVideo.play().then(() => {
              console.log('✓ 视频开始播放');
              if (audioTracks.length > 0) {
                console.log('✓ 音频应该也在播放');
              } else {
                console.warn('⚠ 没有音频轨道');
              }
            }).catch(err => {
              console.error('✗ 视频播放失败:', err);
            });
          } else {
            console.error('✗ 视频元素不存在！');
          }
          
          this.isStreaming = true;
          this.screencastStatus = '正在播放';
          
          if (audioTracks.length > 0 && videoTracks.length > 0) {
            this.$message.success('视频和音频流已连接');
          } else if (videoTracks.length > 0) {
            this.$message.warning('视频流已连接，但没有音频');
          } else {
            this.$message.success('媒体流已连接');
          }
          
          // 更新视频统计信息
          this.updateVideoStats();
        } else {
          console.warn('⚠ 收到track事件但没有streams');
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
      if (!this.mqttClient || !this.isConnected) {
        console.error('MQTT未连接');
        return;
      }

      const topic = `webrtc/${this.currentUsername}/${this.screencastDevice}`;
      
      console.log('>>> 发送信令:', data.type);
      console.log('>>> 主题:', topic);
      console.log('>>> 数据:', JSON.stringify(data).substring(0, 200) + '...');

      this.mqttClient.publish(topic, JSON.stringify(data), { qos: 1 }, (err) => {
        if (err) {
          console.error('发送信令失败:', err);
        } else {
          console.log('✓ 信令发送成功:', data.type);
        }
      });
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
    /** 刷新画面 */
    requestRefresh() {
      if (!this.dataChannel || this.dataChannel.readyState !== 'open') {
        this.$message.warning('请先建立连接');
        return;
      }

      const message = JSON.stringify({
        action: 'refresh'
      });

      this.dataChannel.send(message);
      this.$message.success('已发送刷新请求');
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
        a.download = `screenshot_${this.screencastDevice}_${Date.now()}.png`;
        a.click();
        URL.revokeObjectURL(url);
        this.$message.success('截图已保存');
      });
    },
    /** 更新视频统计信息 */
    updateVideoStats() {
      if (!this.$refs.remoteVideo || !this.$refs.remoteVideo.srcObject) {
        return;
      }

      const video = this.$refs.remoteVideo;
      if (video.videoWidth && video.videoHeight) {
        this.videoStats = `${video.videoWidth}x${video.videoHeight}`;
      }
    },
    /** 清理投屏资源 */
    cleanupScreencast() {
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
      this.videoStats = '';
    }
  }
};
</script>

<style scoped>
.query-connection-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.query-form {
  flex: 1;
  min-width: 400px;
}

.connection-form {
  flex-shrink: 0;
  margin-left: 20px;
}

/* 小屏幕时换行 */
@media (max-width: 1400px) {
  .query-connection-wrapper {
    flex-direction: column;
  }

  .connection-form {
    margin-left: 0;
    margin-top: 10px;
    width: 100%;
    text-align: right;
  }
}

.statistic-item {
  display: flex;
  align-items: center;
  padding: 10px;
}

.statistic-icon {
  font-size: 48px;
  color: #409EFF;
  margin-right: 20px;
}

.statistic-content {
  flex: 1;
}

.statistic-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.statistic-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.statistic-username {
  font-size: 12px;
  color: #67C23A;
  margin-top: 5px;
}

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
