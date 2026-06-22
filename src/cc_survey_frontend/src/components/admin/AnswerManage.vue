<template>
  <div class="answer-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>答卷管理</h2>
        <span class="total-count">共 {{ total }} 份答卷</span>
      </div>
      <el-select v-model="templateFilter" placeholder="筛选问卷" clearable @change="loadData" style="width: 200px">
        <el-option v-for="t in templates" :key="t.uuid" :label="t.title" :value="t.uuid" />
      </el-select>
    </div>

    <!-- 答卷列表 -->
    <div class="answer-list" v-loading="loading">
      <el-table :data="tableData" class="answer-table">
        <el-table-column prop="surveyTitle" label="问卷标题" min-width="200">
          <template #default="{ row }">
            <div class="survey-title-cell">
              <el-icon class="survey-icon"><Document /></el-icon>
              <span>{{ row.surveyTitle }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="填写人" width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="28" class="user-avatar">
                {{ row.name?.charAt(0)?.toUpperCase() || '?' }}
              </el-avatar>
              <span>{{ row.name || '匿名' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="得分" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.totalScore !== null" type="success" effect="light">
              {{ row.totalScore }} 分
            </el-tag>
            <span v-else class="no-score">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Clock /></el-icon>
              <span>{{ formatDate(row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" size="small" @click="handleView(row)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
              <el-button size="small" @click="handleDownload(row)">
                <el-icon><Download /></el-icon>
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @change="loadData"
      class="pagination"
    />

    <!-- 查看详情对话框 -->
    <el-dialog v-model="detailVisible" title="答卷详情" width="700px" destroy-on-close>
      <div class="detail-header">
        <div class="detail-item">
          <span class="label">问卷</span>
          <span class="value">{{ currentAnswer.surveyTitle }}</span>
        </div>
        <div class="detail-item">
          <span class="label">填写人</span>
          <span class="value">{{ currentAnswer.name || '匿名' }}</span>
        </div>
        <div class="detail-item" v-if="currentAnswer.totalScore !== null">
          <span class="label">得分</span>
          <span class="value score">{{ currentAnswer.totalScore }} 分</span>
        </div>
        <div class="detail-item">
          <span class="label">提交时间</span>
          <span class="value">{{ formatDate(currentAnswer.createTime) }}</span>
        </div>
      </div>

      <div class="answer-data" v-if="answerDetails.length">
        <h4>答案详情</h4>
        <div class="answer-list-detail">
          <div
            v-for="(item, index) in answerDetails"
            :key="index"
            class="answer-item"
          >
            <span class="answer-key">{{ item.question }}</span>
            <span class="answer-value">{{ item.value }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无答案数据" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Clock, View, Download, Delete } from '@element-plus/icons-vue'
import api from '@/api/index'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const templateFilter = ref('')
const templates = ref([])

const detailVisible = ref(false)
const currentAnswer = ref({})
const answerDetails = ref([])

onMounted(() => {
  loadTemplates()
  loadData()
})

async function loadTemplates() {
  const res = await api.get('/admin/surveys', { params: { size: 100 } })
  templates.value = res.data.list
}

async function loadData() {
  loading.value = true
  try {
    const res = await api.get('/admin/answers', {
      params: { page: page.value, size: size.value, formId: templateFilter.value }
    })
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleView(row) {
  currentAnswer.value = row
  detailVisible.value = true

  // 获取答卷详情（包含答案数据）
  try {
    const res = await api.get(`/admin/answers/${row.uuid}`)
    currentAnswer.value = res.data

    // 获取问卷schema来显示题目
    if (res.data.templateUuid) {
      const surveyRes = await api.get(`/admin/surveys/${res.data.templateUuid}`)
      let schemaJson = surveyRes.data.schemaJson

      // 如果schemaJson是字符串，解析它
      if (typeof schemaJson === 'string') {
        try {
          schemaJson = JSON.parse(schemaJson)
        } catch (e) {
          console.error('解析schemaJson失败', e)
        }
      }

      console.log('schemaJson:', schemaJson)
      console.log('answerData:', res.data.answerData)

      if (schemaJson && schemaJson.fields) {
        // 构建答案详情列表
        answerDetails.value = buildAnswerDetails(schemaJson.fields, res.data.answerData)
      } else {
        answerDetails.value = []
      }
    }
  } catch (e) {
    console.error('获取答卷详情失败', e)
    answerDetails.value = []
  }
}

function buildAnswerDetails(fields, answerData) {
  if (!answerData) return []

  const details = []

  function processField(field) {
    // 使用与前端提交一致的键名顺序: field.id || field.name
    const fieldName = field.id || field.name
    const label = field.label || fieldName
    const value = answerData[fieldName]

    if (value !== undefined && value !== null) {
      details.push({
        question: label,
        value: formatValue(value, field)
      })
    }

    // 处理子字段
    if (field.subFields && Array.isArray(field.subFields)) {
      field.subFields.forEach(processField)
    }
  }

  fields.forEach(processField)
  return details
}

function formatValue(value, field) {
  if (value === null || value === undefined) return '未填写'
  if (Array.isArray(value)) {
    // 如果是选项类型，尝试显示选项文本
    if (field && field.options) {
      return value.map(v => {
        const option = field.options.find(o => o.value === v)
        return option ? (option.label || v) : v
      }).join(', ') || '未选择'
    }
    return value.join(', ') || '未选择'
  }
  if (typeof value === 'boolean') return value ? '是' : '否'

  // 如果是单选，尝试显示选项文本
  if (field && field.options && typeof value === 'string') {
    const option = field.options.find(o => o.value === value)
    if (option) return option.label || value
  }

  return String(value)
}

function handleDownload(row) {
  window.open(`/api/admin/answers/download/${row.uuid}`, '_blank')
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除该答卷吗？删除后无法恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await api.delete(`/admin/answers/${row.uuid}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleString()
}
</script>

<style scoped lang="scss">
.answer-manage {
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

  .answer-list {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .answer-table {
    .survey-title-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .survey-icon {
        color: #409eff;
      }
    }

    .user-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .user-avatar {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        font-size: 12px;
        flex-shrink: 0;
      }
    }

    .time-cell {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #909399;
      font-size: 13px;
    }

    .no-score {
      color: #c0c4cc;
    }

    .action-btns {
      display: flex;
      gap: 8px;
    }
  }

  .pagination {
    margin-top: 20px;
    padding-bottom: 20px;
    justify-content: center;
  }

  .detail-header {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 20px;

    .detail-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .label {
        font-size: 12px;
        color: #909399;
      }

      .value {
        font-size: 14px;
        color: #303133;
        font-weight: 500;

        &.score {
          color: #67c23a;
          font-size: 18px;
        }
      }
    }
  }

  .answer-data {
    h4 {
      margin: 0 0 15px;
      font-size: 15px;
      color: #606266;
    }

    .answer-list-detail {
      max-height: 400px;
      overflow-y: auto;
    }

    .answer-item {
      display: flex;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .answer-key {
        width: 150px;
        flex-shrink: 0;
        color: #606266;
        font-size: 13px;
      }

      .answer-value {
        flex: 1;
        color: #303133;
        font-size: 13px;
        word-break: break-all;
      }
    }
  }
}
</style>