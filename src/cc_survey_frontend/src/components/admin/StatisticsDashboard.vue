<template>
  <div class="statistics-dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon users">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
          <div class="stat-label">总用户数</div>
          <div class="stat-sub">今日新增: {{ stats.todayNewUsers || 0 }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon surveys">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalSurveys || 0 }}</div>
          <div class="stat-label">总问卷数</div>
          <div class="stat-sub">已发布: {{ stats.publishedSurveys || 0 }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon submissions">
          <el-icon><List /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalSubmissions || 0 }}</div>
          <div class="stat-label">总提交数</div>
          <div class="stat-sub">今日提交: {{ stats.todaySubmissions || 0 }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <div class="chart-card trend-card">
        <div class="card-header">
          <h3>最近7天提交趋势</h3>
        </div>
        <div class="trend-chart" v-if="stats.submissionTrend?.length">
          <div
            v-for="item in stats.submissionTrend"
            :key="item.date"
            class="trend-bar-wrapper"
          >
            <div class="trend-bar" :style="{ height: getBarHeight(item.count) + '%' }">
              <span class="trend-value">{{ item.count }}</span>
            </div>
            <span class="trend-label">{{ formatDateShort(item.date) }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无数据" />
      </div>

      <div class="chart-card ranking-card">
        <div class="card-header">
          <h3>问卷提交排行</h3>
        </div>
        <div class="ranking-list" v-if="stats.surveyRanking?.length">
          <div v-for="(item, index) in stats.surveyRanking" :key="item.uuid" class="ranking-item">
            <span class="ranking-index" :class="'top-' + (index + 1)">{{ index + 1 }}</span>
            <span class="ranking-title">{{ item.title }}</span>
            <span class="ranking-count">{{ item.count }} 次</span>
          </div>
        </div>
        <el-empty v-else description="暂无数据" />
      </div>
    </div>

    <!-- 用户排行 -->
    <div class="chart-card">
      <div class="card-header">
        <h3>用户活跃排行</h3>
      </div>
      <div class="user-ranking" v-if="stats.userRanking?.length">
        <div v-for="(item, index) in stats.userRanking" :key="item.uuid" class="user-item">
          <span class="user-index" :class="'top-' + (index + 1)">{{ index + 1 }}</span>
          <el-avatar :size="36" class="user-avatar">
            {{ item.name?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <span class="user-name">{{ item.name }}</span>
          <span class="user-count">{{ item.count }} 次提交</span>
        </div>
      </div>
      <el-empty v-else description="暂无数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { User, Document, List } from '@element-plus/icons-vue'
import api from '@/api/index'

const stats = ref({})

onMounted(() => {
  loadStats()
})

async function loadStats() {
  const res = await api.get('/admin/statistics/dashboard')
  stats.value = res.data
}

function getBarHeight(count) {
  if (!stats.value.submissionTrend) return 0
  const max = Math.max(...stats.value.submissionTrend.map(i => i.count), 1)
  return (count / max) * 100
}

function formatDateShort(date) {
  if (!date) return ''
  // Format as MM-DD to ensure consistent width
  const parts = date.split('-')
  if (parts.length >= 3) {
    return parts[1] + '-' + parts[2]
  }
  return date.substring(5)
}
</script>

<style scoped lang="scss">
.statistics-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  height: 100%;
  overflow-y: auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  }

  .stat-icon {
    width: 64px;
    height: 64px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: #fff;
    flex-shrink: 0;

    &.users { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
    &.surveys { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
    &.submissions { background: linear-gradient(135deg, #ee0979 0%, #ff6a00 100%); }
    &.score { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
  }

  .stat-info {
    flex: 1;

    .stat-value {
      font-size: 32px;
      font-weight: 700;
      color: #303133;
    }

    .stat-label {
      color: #909399;
      font-size: 14px;
      margin-top: 4px;
    }

    .stat-sub {
      color: #c0c4cc;
      font-size: 12px;
      margin-top: 4px;
    }
  }
}

.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;

  @media (max-width: 1000px) {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);

  .card-header {
    margin-bottom: 20px;

    h3 {
      margin: 0;
      font-size: 16px;
      color: #303133;
      font-weight: 600;
    }
  }
}

.trend-card {
  .trend-chart {
    display: flex;
    align-items: flex-end;
    height: 220px;
    gap: 16px;
    padding-top: 30px;

    .trend-bar-wrapper {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: flex-end;
      height: 100%;

      .trend-bar {
        width: 100%;
        background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
        border-radius: 8px 8px 0 0;
        display: flex;
        justify-content: center;
        min-height: 20px;
        position: relative;
        transition: height 0.3s;

        .trend-value {
          position: absolute;
          top: -24px;
          font-size: 13px;
          color: #606266;
          font-weight: 500;
          white-space: nowrap;
        }
      }

      .trend-label {
        margin-top: 8px;
        font-size: 12px;
        color: #909399;
        white-space: nowrap;
        min-width: 50px;
        text-align: center;
        flex-shrink: 0;
      }
    }
  }
}

.ranking-card {
  .ranking-list {
    max-height: 280px;
    overflow-y: auto;
  }

  .ranking-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .ranking-index {
      width: 28px;
      height: 28px;
      border-radius: 8px;
      background: #f0f0f0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      color: #909399;
      margin-right: 12px;
      font-weight: 500;

      &.top-1 { background: linear-gradient(135deg, #ffd700 0%, #ffec8b 100%); color: #fff; }
      &.top-2 { background: linear-gradient(135deg, #c0c0c0 0%, #e8e8e8 100%); color: #fff; }
      &.top-3 { background: linear-gradient(135deg, #cd7f32 0%, #daa06d 100%); color: #fff; }
    }

    .ranking-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 14px;
      color: #303133;
    }

    .ranking-count {
      color: #909399;
      font-size: 13px;
      margin-left: 10px;
    }
  }
}

.user-ranking {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;

  .user-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 12px;
    min-width: 220px;
    transition: background 0.2s;

    &:hover {
      background: #eef2f7;
    }

    .user-index {
      width: 24px;
      height: 24px;
      border-radius: 6px;
      background: #e4e7ed;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: #909399;
      font-weight: 500;

      &.top-1 { background: linear-gradient(135deg, #ffd700 0%, #ffec8b 100%); color: #fff; }
      &.top-2 { background: linear-gradient(135deg, #c0c0c0 0%, #e8e8e8 100%); color: #fff; }
      &.top-3 { background: linear-gradient(135deg, #cd7f32 0%, #daa06d 100%); color: #fff; }
    }

    .user-avatar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      font-weight: 600;
    }

    .user-name {
      flex: 1;
      font-size: 14px;
      color: #303133;
    }

    .user-count {
      color: #909399;
      font-size: 12px;
    }
  }
}
</style>