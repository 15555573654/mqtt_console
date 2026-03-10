/**
 * WebRTC 连接管理器
 * 负责处理 WebRTC 连接、信令交换、媒体流管理等
 */
export default class WebRTCManager {
  constructor(options = {}) {
    this.deviceName = options.deviceName || '';
    this.mqttClient = options.mqttClient || null;
    this.username = options.username || '';
    
    // ICE 服务器配置
    this.iceServers = options.iceServers || [
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
    ];
    
    // WebRTC 对象
    this.peerConnection = null;
    this.dataChannel = null;
    this.remoteStream = null;
    
    // 状态
    this.connectionState = 'disconnected'; // disconnected, connecting, connected
    
    // 事件回调
    this.callbacks = {
      onConnectionStateChange: null,
      onTrack: null,
      onDataChannelMessage: null,
      onIceCandidate: null,
      onError: null
    };
  }
  
  /**
   * 设置事件回调
   */
  on(event, callback) {
    if (this.callbacks.hasOwnProperty(`on${event.charAt(0).toUpperCase()}${event.slice(1)}`)) {
      this.callbacks[`on${event.charAt(0).toUpperCase()}${event.slice(1)}`] = callback;
    }
  }
  
  /**
   * 更新配置
   */
  updateConfig(config) {
    if (config.deviceName) this.deviceName = config.deviceName;
    if (config.mqttClient) this.mqttClient = config.mqttClient;
    if (config.username) this.username = config.username;
  }
  
  /**
   * 创建 PeerConnection
   */
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
    
    // ICE 候选事件
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
      
      this.connectionState = this.peerConnection.connectionState;
      
      if (this.callbacks.onConnectionStateChange) {
        this.callbacks.onConnectionStateChange(this.connectionState);
      }
      
      if (this.connectionState === 'connected') {
        console.log('✓ WebRTC连接成功！');
      } else if (this.connectionState === 'failed' || this.connectionState === 'disconnected') {
        console.error('✗ WebRTC连接失败或断开');
      }
    };
    
    // ICE 连接状态变化
    this.peerConnection.oniceconnectionstatechange = () => {
      console.log('>>> ICE连接状态变化:', this.peerConnection.iceConnectionState);
      
      if (this.peerConnection.iceConnectionState === 'connected' || 
          this.peerConnection.iceConnectionState === 'completed') {
        console.log('✓ ICE连接成功！');
      } else if (this.peerConnection.iceConnectionState === 'failed') {
        console.error('✗ ICE连接失败 - 可能是NAT/防火墙问题');
        if (this.callbacks.onError) {
          this.callbacks.onError('ICE连接失败，请检查网络配置');
        }
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
        
        if (this.callbacks.onTrack) {
          this.callbacks.onTrack(this.remoteStream, { audioTracks, videoTracks });
        }
      }
    };
    
    // DataChannel 接收
    this.peerConnection.ondatachannel = (event) => {
      const channel = event.channel;
      channel.onmessage = (e) => {
        this.handleDataChannelMessage(e.data);
      };
    };
  }
  
  /**
   * 创建 DataChannel
   */
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
      if (this.callbacks.onError) {
        this.callbacks.onError('DataChannel错误: ' + error.message);
      }
    };
  }
  
  /**
   * 处理 DataChannel 消息
   */
  handleDataChannelMessage(data) {
    try {
      const message = JSON.parse(data);
      console.log('收到DataChannel消息:', message);
      
      if (this.callbacks.onDataChannelMessage) {
        this.callbacks.onDataChannelMessage(message);
      }
    } catch (e) {
      console.log('收到非JSON消息:', data);
    }
  }
  
  /**
   * 发送信令消息
   */
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
  }
  
  /**
   * 开始连接
   */
  async start() {
    try {
      this.connectionState = 'connecting';
      
      // 创建 PeerConnection
      await this.createPeerConnection();
      
      // 创建 DataChannel
      this.createDataChannel();
      
      // 创建 Offer
      const offer = await this.peerConnection.createOffer();
      await this.peerConnection.setLocalDescription(offer);
      
      // 通过 MQTT 发送 Offer
      this.sendSignaling({
        type: 'offer',
        deviceName: this.deviceName,
        offer: offer
      });
      
      return true;
    } catch (error) {
      console.error('开始连接失败:', error);
      this.connectionState = 'disconnected';
      if (this.callbacks.onError) {
        this.callbacks.onError('开始连接失败: ' + error.message);
      }
      throw error;
    }
  }
  
  /**
   * 处理 Answer
   */
  async handleAnswer(answer) {
    console.log('>>> 开始设置远程描述（Answer）');
    await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
    console.log('✓ 远程描述设置成功');
  }
  
  /**
   * 处理 ICE 候选
   */
  async handleIceCandidate(candidate) {
    console.log('>>> 添加远程ICE候选');
    if (this.peerConnection) {
      await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      console.log('✓ ICE候选添加成功');
    }
  }
  
  /**
   * 发送 DataChannel 消息
   */
  sendMessage(message) {
    if (!this.dataChannel || this.dataChannel.readyState !== 'open') {
      console.warn('DataChannel未打开');
      return false;
    }
    
    const data = typeof message === 'string' ? message : JSON.stringify(message);
    this.dataChannel.send(data);
    return true;
  }
  
  /**
   * 发送虚拟按键
   */
  sendVirtualKey(key) {
    return this.sendMessage({
      action: 'virtualKey',
      key: key
    });
  }
  
  /**
   * 请求屏幕共享
   */
  requestScreenShare() {
    return this.sendMessage({
      action: 'startScreenShare'
    });
  }
  
  /**
   * 更新视频设置
   */
  updateVideoSettings(settings) {
    return this.sendMessage({
      action: 'updateVideoSettings',
      settings: settings
    });
  }
  
  /**
   * 停止连接
   */
  stop() {
    this.cleanup();
    this.connectionState = 'disconnected';
  }
  
  /**
   * 清理资源
   */
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
  }
  
  /**
   * 获取连接状态
   */
  getConnectionState() {
    return this.connectionState;
  }
  
  /**
   * 获取远程流
   */
  getRemoteStream() {
    return this.remoteStream;
  }
  
  /**
   * 检查 DataChannel 是否可用
   */
  isDataChannelOpen() {
    return this.dataChannel && this.dataChannel.readyState === 'open';
  }
}
