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

    <!-- 查询表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
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

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-video-play"
          size="mini"
          :disabled="multiple"
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
          :disabled="multiple"
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
          :disabled="multiple"
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
          :disabled="multiple"
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
          :disabled="multiple"
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
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mqtt:device:remove']"
        >删除设备</el-button>
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
  </div>
</template>

<script>
import { listDevice, delDevice, sendCommand, getStatistics } from "@/api/mqtt/device";

export default {
  name: "MqttDevice",
  data() {
    return {
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
      statistics: {},
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
    this.getList();
    this.getStatistics();
    // 每10秒刷新一次数据
    this.timer = setInterval(() => {
      this.refreshData();
    }, 10000);
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  },
  methods: {
    /** 查询设备列表 */
    getList() {
      this.loading = true;
      listDevice(this.queryParams).then(response => {
        this.deviceList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 获取统计信息 */
    getStatistics() {
      getStatistics().then(response => {
        this.statistics = response.data;
      });
    },
    /** 刷新数据（列表+统计） */
    refreshData() {
      this.getList();
      this.getStatistics();
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
        return sendCommand({
          action: action,
          deviceNames: this.deviceNames
        });
      }).then(() => {
        this.getList();
        this.getStatistics();
        this.$modal.msgSuccess(actionNames[action] + "命令已发送");
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
    }
  }
};
</script>

<style scoped>
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
</style>
