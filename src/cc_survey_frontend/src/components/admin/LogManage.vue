<template>
  <div class="log-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>操作日志</h2>
        <span class="total-count">共 {{ total }} 条记录</span>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input
        v-model="titleFilter"
        placeholder="操作模块"
        clearable
        style="width: 150px"
        @keyup.enter="loadData"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select v-model="typeFilter" placeholder="业务类型" clearable @change="loadData" style="width: 120px">
        <el-option label="新增" value="INSERT" />
        <el-option label="修改" value="UPDATE" />
        <el-option label="删除" value="DELETE" />
        <el-option label="查询" value="SELECT" />
        <el-option label="导出" value="EXPORT" />
      </el-select>

      <el-select v-model="statusFilter" placeholder="状态" clearable @change="loadData" style="width: 100px">
        <el-option label="成功" :value="0" />
        <el-option label="失败" :value="1" />
      </el-select>

      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="loadData"
        style="width: 260px"
      />
    </div>

    <!-- 日志列表 -->
    <div class="log-list" v-loading="loading">
      <el-table :data="tableData" class="log-table">
        <el-table-column prop="title" label="操作模块" width="120">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.title }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="businessType" label="业务类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getBusinessTypeColor(row.businessType)" effect="light" size="small">
              {{ row.businessType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operName" label="操作人" width="100">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="24" class="user-avatar">
                {{ row.operName?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span>{{ row.operName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="operIp" label="IP地址" width="130" />
        <el-table-column prop="requestMethod" label="请求方式" width="80">
          <template #default="{ row }">
            <el-tag :type="getMethodColor(row.requestMethod)" size="small">
              {{ row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operUrl" label="请求URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" effect="light">
              {{ row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时" width="90">
          <template #default="{ row }">
            <span class="cost-time" :class="{ slow: row.costTime > 500 }">
              {{ row.costTime }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="operTime" label="操作时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.operTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              <el-icon><View /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @change="loadData"
      class="pagination"
    />

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="700px" destroy-on-close>
      <div class="detail-header">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">操作模块</span>
            <span class="value">{{ currentLog.title }}</span>
          </div>
          <div class="detail-item">
            <span class="label">业务类型</span>
            <span class="value">{{ currentLog.businessType }}</span>
          </div>
          <div class="detail-item">
            <span class="label">操作人</span>
            <span class="value">{{ currentLog.operName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">IP地址</span>
            <span class="value">{{ currentLog.operIp }}</span>
          </div>
          <div class="detail-item">
            <span class="label">请求方式</span>
            <span class="value">{{ currentLog.requestMethod }}</span>
          </div>
          <div class="detail-item">
            <span class="label">耗时</span>
            <span class="value">{{ currentLog.costTime }}ms</span>
          </div>
          <div class="detail-item full">
            <span class="label">请求URL</span>
            <span class="value">{{ currentLog.operUrl }}</span>
          </div>
          <div class="detail-item full">
            <span class="label">操作时间</span>
            <span class="value">{{ formatDate(currentLog.operTime) }}</span>
          </div>
        </div>
      </div>

      <div class="log-detail" v-if="currentLog.operParam">
        <h4>请求参数</h4>
        <pre>{{ currentLog.operParam }}</pre>
      </div>

      <div class="log-detail" v-if="currentLog.jsonResult">
        <h4>返回结果</h4>
        <pre>{{ currentLog.jsonResult }}</pre>
      </div>

      <div class="log-detail error" v-if="currentLog.errorMsg">
        <h4>错误信息</h4>
        <pre>{{ currentLog.errorMsg }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, View } from '@element-plus/icons-vue'
import api from '@/api/index'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const titleFilter = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const dateRange = ref(null)

const detailVisible = ref(false)
const currentLog = ref({})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      title: titleFilter.value,
      businessType: typeFilter.value,
      status: statusFilter.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0].toISOString()
      params.endTime = dateRange.value[1].toISOString()
    }

    const res = await api.get('/admin/logs', { params })
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleView(row) {
  currentLog.value = row
  detailVisible.value = true
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleString()
}

function getBusinessTypeColor(type) {
  const colors = {
    INSERT: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    SELECT: 'primary',
    EXPORT: 'info'
  }
  return colors[type] || 'info'
}

function getMethodColor(method) {
  const colors = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return colors[method] || 'info'
}
</script>

<style scoped lang="scss">
.log-manage {
  padding: 24px;
  height: 100%;
  overflow-y: auto;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .header-left {
      display: flex;
      align-items: baseline;
      gap: 15px;

      h2 {
        margin: 0;
        font-size: 22px;
        color: #303133;
      }

      .total-count {
        color: #909399;
        font-size: 14px;
      }
    }
  }

  .filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
    flex-wrap: wrap;
  }

  .log-list {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .log-table {
    .user-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .user-avatar {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        font-size: 11px;
        flex-shrink: 0;
      }
    }

    .time-text {
      color: #909399;
      font-size: 13px;
    }

    .cost-time {
      color: #606266;
      font-size: 13px;

      &.slow {
        color: #f56c6c;
      }
    }
  }

  .pagination {
    margin-top: 20px;
    padding-bottom: 20px;
    justify-content: center;
  }

  .detail-header {
    padding: 20px;
    background: #f5f7fa;
    border-radius: 12px;
    margin-bottom: 20px;

    .detail-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
    }

    .detail-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      &.full {
        grid-column: span 2;
      }

      .label {
        font-size: 12px;
        color: #909399;
      }

      .value {
        font-size: 14px;
        color: #303133;
      }
    }
  }

  .log-detail {
    margin-top: 20px;

    h4 {
      margin: 0 0 10px;
      font-size: 14px;
      color: #606266;
    }

    pre {
      background: #f5f7fa;
      padding: 15px;
      border-radius: 8px;
      max-height: 200px;
      overflow: auto;
      font-size: 12px;
      line-height: 1.5;
    }

    &.error {
      pre {
        background: #fef0f0;
        color: #f56c6c;
      }
    }
  }
}
</style>