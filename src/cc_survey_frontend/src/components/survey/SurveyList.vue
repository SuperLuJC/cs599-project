<template>
  <div class="survey-list-page">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-content">
        <h1>问卷列表</h1>
        <p>选择问卷开始填写</p>
      </div>
      <div class="header-actions">
        <el-button v-if="authStore.isAdmin" type="primary" @click="goToAdmin">
          <el-icon><Setting /></el-icon>
          管理后台
        </el-button>
      </div>
    </header>

    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input
        v-model="keyword"
        placeholder="搜索问卷..."
        clearable
        size="large"
        class="search-input"
        @keyup.enter="loadData"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" size="large" @click="loadData">搜索</el-button>
    </div>

    <!-- 问卷列表 -->
    <div class="survey-list" v-loading="loading">
      <div
        v-for="survey in surveys"
        :key="survey.uuid"
        class="survey-card"
        @click="handleStart(survey)"
      >
        <div class="card-header">
          <span class="status-tag" :class="getSurveyStatusClass(survey)">
            {{ getSurveyStatusText(survey) }}
          </span>
        </div>

        <h3 class="survey-title">{{ survey.title }}</h3>
        <p class="survey-desc">{{ survey.description || '暂无描述' }}</p>

        <div class="card-meta">
          <span class="meta-item">
            <el-icon><User /></el-icon>
            {{ survey.submissionCount || 0 }} 人已填写
          </span>
          <span class="meta-item" v-if="survey.createdByName">
            <el-icon><Edit /></el-icon>
            {{ survey.createdByName }}
          </span>
          <span class="meta-item">
            <el-icon><Calendar /></el-icon>
            {{ formatDate(survey.createTime) }}
          </span>
        </div>

        <div class="card-action">
          <el-button type="primary" class="start-btn">
            开始填写
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>

      <div v-if="!loading && surveys.length === 0" class="empty-state">
        <el-icon :size="64" color="#c0c4cc"><Document /></el-icon>
        <p>暂无可填写的问卷</p>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-section" v-if="total > size">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 30]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Document, User, Calendar, ArrowRight, Setting, Edit } from '@element-plus/icons-vue'
import api from '@/api/index'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const surveys = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await api.get('/surveys/list', {
      params: { page: page.value, size: size.value, keyword: keyword.value }
    })
    surveys.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleStart(survey) {
  router.push(`/survey/${survey.uuid}`)
}

function goToAdmin() {
  router.push('/admin/surveys')
}

function getSurveyStatusClass(survey) {
  const now = Date.now()
  if (survey.startTime && new Date(survey.startTime).getTime() > now) {
    return 'not-started'
  }
  if (survey.endTime && new Date(survey.endTime).getTime() < now) {
    return 'ended'
  }
  return survey.status === 1 ? 'active' : 'ended'
}

function getSurveyStatusText(survey) {
  const now = Date.now()
  if (survey.startTime && new Date(survey.startTime).getTime() > now) {
    return '未开始'
  }
  if (survey.endTime && new Date(survey.endTime).getTime() < now) {
    return '已结束'
  }
  return survey.status === 1 ? '进行中' : '已结束'
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString()
}
</script>

<style scoped lang="scss">
.survey-list-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px;
}

.page-header {
  max-width: 900px;
  margin: 0 auto 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-content {
    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #1a1a1a;
    }
    p {
      margin: 4px 0 0;
      font-size: 14px;
      color: #666;
    }
  }
}

.search-section {
  max-width: 900px;
  margin: 0 auto 24px;
  display: flex;
  gap: 12px;

  .search-input {
    flex: 1;
  }
}

.survey-list {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.survey-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);

    .start-btn {
      background: #1a73e8;
    }
  }

  .card-header {
    margin-bottom: 12px;
  }

  .status-tag {
    display: inline-block;
    padding: 4px 10px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;

    &.active {
      background: #e8f5e9;
      color: #2e7d32;
    }

    &.ended {
      background: #f5f5f5;
      color: #757575;
    }

    &.not-started {
      background: #fff3e0;
      color: #e65100;
    }
  }

  .survey-title {
    margin: 0 0 8px;
    font-size: 18px;
    font-weight: 600;
    color: #1a1a1a;
    line-height: 1.4;
  }

  .survey-desc {
    margin: 0 0 16px;
    font-size: 14px;
    color: #666;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-meta {
    display: flex;
    gap: 20px;
    margin-bottom: 16px;
    font-size: 13px;
    color: #888;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .card-action {
    display: flex;
    justify-content: flex-end;

    .start-btn {
      padding: 8px 20px;
      font-size: 14px;
      background: #409eff;
      border: none;
      border-radius: 6px;
      transition: background 0.2s;
    }
  }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;

  p {
    margin: 16px 0 24px;
    font-size: 16px;
  }
}

.pagination-section {
  max-width: 900px;
  margin: 24px auto 0;
  padding-bottom: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .survey-list-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .survey-card {
    padding: 16px;

    .survey-title {
      font-size: 16px;
    }
  }
}
</style>
