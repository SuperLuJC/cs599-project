<template>
  <div class="survey-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>问卷管理</h2>
        <span class="total-count">共 {{ total }} 份问卷</span>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建问卷
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-radio-group v-model="statusFilter" @change="loadData">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">草稿</el-radio-button>
        <el-radio-button :value="1">已发布</el-radio-button>
        <el-radio-button :value="2">已归档</el-radio-button>
      </el-radio-group>

      <el-input
        v-model="keyword"
        placeholder="搜索问卷标题"
        clearable
        style="width: 240px"
        @keyup.enter="loadData"
      >
        <template #append>
          <el-button @click="loadData">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- 问卷列表 -->
    <div class="survey-list" v-loading="loading">
      <div
        v-for="survey in tableData"
        :key="survey.uuid"
        class="survey-card"
      >
        <div class="card-header">
          <el-tag :type="getStatusType(survey.status)" effect="dark" size="small">
            {{ getStatusText(survey.status) }}
          </el-tag>
          <el-dropdown trigger="click">
            <el-button link>
              <el-icon :size="18"><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleEdit(survey)">
                  <el-icon><Edit /></el-icon> 编辑
                </el-dropdown-item>
                <el-dropdown-item @click="handlePreview(survey)">
                  <el-icon><View /></el-icon> 预览
                </el-dropdown-item>
                <el-dropdown-item @click="handleCopy(survey)" divided>
                  <el-icon><CopyDocument /></el-icon> 复制
                </el-dropdown-item>
                <el-dropdown-item @click="handleDelete(survey)" divided>
                  <el-icon color="#f56c6c"><Delete /></el-icon>
                  <span style="color: #f56c6c">删除</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <h3 class="survey-title">{{ survey.title }}</h3>
        <p class="survey-desc">{{ survey.description || '暂无描述' }}</p>

        <div class="survey-meta">
          <div class="meta-item">
            <span class="meta-value">{{ survey.submissionCount || 0 }}</span>
            <span class="meta-label">提交数</span>
          </div>
          <div class="meta-item" v-if="survey.createdByName">
            <span class="meta-value">{{ survey.createdByName }}</span>
            <span class="meta-label">创建人</span>
          </div>
        </div>

        <div class="card-footer">
          <span class="create-time">{{ formatDate(survey.createTime) }}</span>
          <div class="actions">
            <el-button
              v-if="survey.status === 0"
              type="success"
              size="small"
              @click="handlePublish(survey)"
            >
              发布
            </el-button>
            <el-button
              v-if="survey.status === 1"
              type="warning"
              size="small"
              @click="handleArchive(survey)"
            >
              归档
            </el-button>
            <el-button
              v-if="survey.status === 1"
              type="primary"
              size="small"
              plain
              @click="handleViewAnswers(survey)"
            >
              查看答卷
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无问卷" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MoreFilled, Edit, View, CopyDocument, Delete } from '@element-plus/icons-vue'
import api from '@/api/index'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref(null)
const keyword = ref('')

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await api.get('/admin/surveys', {
      params: {
        page: page.value,
        size: size.value,
        status: statusFilter.value,
        keyword: keyword.value
      }
    })
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  router.push('/admin/surveys/create')
}

function handleEdit(row) {
  router.push(`/admin/surveys/${row.uuid}/edit`)
}

function handlePreview(row) {
  window.open(`/survey/${row.uuid}`, '_blank')
}

function handleViewAnswers(row) {
  router.push({ path: '/admin/answers', query: { formId: row.uuid } })
}

async function handlePublish(row) {
  await ElMessageBox.confirm('确定要发布该问卷吗？发布后用户即可填写', '发布确认')
  await api.post(`/admin/surveys/${row.uuid}/publish`)
  ElMessage.success('发布成功')
  loadData()
}

async function handleArchive(row) {
  await ElMessageBox.confirm('确定要归档该问卷吗？归档后将停止收集', '归档确认')
  await api.post(`/admin/surveys/${row.uuid}/archive`)
  ElMessage.success('归档成功')
  loadData()
}

async function handleCopy(row) {
  await api.post(`/admin/surveys/${row.uuid}/copy`)
  ElMessage.success('复制成功')
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定要删除该问卷吗？删除后无法恢复', '删除确认', { type: 'warning' })
  await api.delete(`/admin/surveys/${row.uuid}`)
  ElMessage.success('删除成功')
  loadData()
}

function getStatusType(status) {
  const types = { 0: 'info', 1: 'success', 2: 'warning' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { 0: '草稿', 1: '已发布', 2: '已归档' }
  return texts[status] || '未知'
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString()
}
</script>

<style scoped lang="scss">
.survey-manage {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .header-left {
    display: flex;
    align-items: baseline;
    gap: 12px;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #1a1a1a;
    }

    .total-count {
      font-size: 14px;
      color: #888;
    }
  }
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}

.survey-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.survey-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
  display: flex;
  flex-direction: column;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .survey-title {
    margin: 0 0 6px;
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .survey-desc {
    margin: 0 0 12px;
    font-size: 13px;
    color: #888;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    flex: 1;
  }

  .survey-meta {
    display: flex;
    gap: 20px;
    padding: 12px 0;
    border-top: 1px solid #f0f0f0;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 12px;

    .meta-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .meta-value {
        font-size: 18px;
        font-weight: 600;
        color: #409eff;
      }

      .meta-label {
        font-size: 12px;
        color: #888;
        margin-top: 2px;
      }
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .create-time {
      font-size: 12px;
      color: #bbb;
    }

    .actions {
      display: flex;
      gap: 8px;
    }
  }
}

.pagination {
  margin-top: 24px;
  padding-bottom: 24px;
  justify-content: center;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .survey-list {
    grid-template-columns: 1fr;
  }
}
</style>
