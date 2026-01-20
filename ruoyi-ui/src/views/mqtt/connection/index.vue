<template>
  <div class="app-container">
    <el-card shadow="hover">
      <div slot="header" class="clearfix">
        <span>MQTT连接设置</span>
      </div>

      <!-- 连接状态 -->
      <el-alert
        :title="connectionStatus"
        :type="isConnected ? 'success' : 'warning'"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />

      <!-- 连接表单 -->
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名（至少5位）" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button
            v-if="!isConnected"
            type="primary"
            @click="handleConnect"
            :loading="connecting"
            v-hasPermi="['mqtt:connection:connect']"
          >连接MQTT</el-button>
          <el-button
            v-else
            type="danger"
            @click="handleDisconnect"
            v-hasPermi="['mqtt:connection:disconnect']"
          >断开连接</el-button>
          <el-button @click="handleRefresh">刷新状态</el-button>
        </el-form-item>
      </el-form>

      <!-- 连接信息 -->
      <el-divider></el-divider>
      <el-descriptions title="连接信息" :column="2" border>
        <el-descriptions-item label="连接状态">
          <el-tag :type="isConnected ? 'success' : 'info'">
            {{ isConnected ? '已连接' : '未连接' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前用户">
          {{ currentUsername || '未连接' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 使用说明 -->
      <el-divider></el-divider>
      <div class="help-text">
        <h4>使用说明：</h4>
        <ol>
          <li>输入用户名（至少5位）和密码</li>
          <li>点击"连接MQTT"按钮建立连接</li>
          <li>连接成功后，系统将自动订阅设备主题</li>
          <li>设备上线后会自动显示在设备列表中</li>
          <li>可以通过设备管理页面控制设备</li>
        </ol>
      </div>
    </el-card>
  </div>
</template>

<script>
import { connect, disconnect, getStatus } from "@/api/mqtt/connection";

export default {
  name: "MqttConnection",
  data() {
    return {
      // 连接状态
      isConnected: false,
      // 当前用户名
      currentUsername: '',
      // 连接中
      connecting: false,
      // 表单参数
      form: {
        username: 'test002',
        password: 'test003'
      },
      // 表单校验
      rules: {
        username: [
          { required: true, message: "用户名不能为空", trigger: "blur" },
          { min: 5, message: "用户名长度至少5位", trigger: "blur" }
        ],
        password: [
          { required: true, message: "密码不能为空", trigger: "blur" }
        ]
      }
    };
  },
  computed: {
    connectionStatus() {
      return this.isConnected ? 'MQTT连接正常' : 'MQTT未连接';
    }
  },
  created() {
    this.getStatus();
  },
  methods: {
    /** 获取连接状态 */
    getStatus() {
      getStatus().then(response => {
        this.isConnected = response.data.connected;
        this.currentUsername = response.data.username;
      });
    },
    /** 连接MQTT */
    handleConnect() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.connecting = true;
          connect(this.form).then(response => {
            this.$modal.msgSuccess("MQTT连接成功");
            this.getStatus();
            this.connecting = false;
          }).catch(() => {
            this.connecting = false;
          });
        }
      });
    },
    /** 断开连接 */
    handleDisconnect() {
      this.$modal.confirm('是否确认断开MQTT连接？').then(() => {
        return disconnect();
      }).then(() => {
        this.$modal.msgSuccess("MQTT连接已断开");
        this.getStatus();
      }).catch(() => {});
    },
    /** 刷新状态 */
    handleRefresh() {
      this.getStatus();
      this.$modal.msgSuccess("状态已刷新");
    }
  }
};
</script>

<style scoped>
.help-text {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.help-text h4 {
  margin-top: 0;
  color: #303133;
}

.help-text ol {
  margin: 10px 0;
  padding-left: 20px;
}

.help-text li {
  margin: 8px 0;
  color: #606266;
}
</style>
