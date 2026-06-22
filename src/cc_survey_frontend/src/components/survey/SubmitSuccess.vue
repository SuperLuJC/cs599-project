<template>
  <div class="success-page">
    <div class="success-card">
      <div class="success-icon">
        <el-icon :size="64" color="#67c23a"><CircleCheckFilled /></el-icon>
      </div>

      <h2>提交成功</h2>
      <p>{{ showScore ? '感谢您的参与！您的答案已成功提交。' : '感谢您的参与！您的答案已成功提交，即将返回首页。' }}</p>

      <template v-if="showScore">
        <div class="score-section" v-if="result">
          <div class="score-main">
            <span class="score-value">{{ result.totalScore }}</span>
            <span class="score-unit">分</span>
          </div>
          <div class="score-max">满分 {{ result.maxScore }} 分</div>
        </div>

        <div class="answers-section" v-if="result?.answers?.length">
          <h3>答案详情</h3>
          <div class="answers-list">
            <div
              v-for="(answer, index) in result.answers"
              :key="index"
              class="answer-row"
            >
              <div class="answer-q">{{ answer.question }}</div>
              <div class="answer-a">
                <span class="answer-text">{{ formatAnswer(answer.value) }}</span>
                <span class="answer-score" :class="answer.isCorrect ? 'correct' : 'wrong'">
                  {{ answer.score }} 分
                </span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <div class="actions">
        <el-button @click="handleBack">返回列表</el-button>
        <el-button type="primary" @click="handleViewSurvey">查看问卷</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import api from '@/api/index'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const result = ref(null)
const showScore = ref(false)
const surveyId = route.params.id

onMounted(() => {
  loadResult()
})

async function loadResult() {
  try {
    const res = await api.get(`/surveys/${surveyId}/my-submission`)
    result.value = res.data
    showScore.value = res.data?.showScore === true
  } catch (error) {
    console.log('No result data available')
  }
}

function formatAnswer(value) {
  if (value === null || value === undefined) return '未填写'
  if (Array.isArray(value)) {
    return value.join(', ') || '未选择'
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  return String(value)
}

function handleBack() {
  // 根据角色返回对应的主界面
  if (authStore.isAdmin) {
    router.push('/admin/surveys')
  } else {
    router.push('/')
  }
}

function handleViewSurvey() {
  router.push(`/survey/${surveyId}`)
}
</script>

<style scoped lang="scss">
.success-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.success-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  max-width: 480px;
  width: 100%;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.success-icon {
  margin-bottom: 16px;
}

h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

p {
  margin: 0 0 24px;
  font-size: 14px;
  color: #666;
}

.score-section {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border-radius: 12px;
  padding: 24px;
  color: #fff;
  margin-bottom: 24px;

  .score-main {
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: 4px;
  }

  .score-value {
    font-size: 48px;
    font-weight: 700;
    line-height: 1;
  }

  .score-unit {
    font-size: 18px;
    opacity: 0.9;
  }

  .score-max {
    margin-top: 8px;
    font-size: 14px;
    opacity: 0.8;
  }
}

.answers-section {
  text-align: left;
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;

  h3 {
    margin: 0 0 12px;
    font-size: 14px;
    font-weight: 600;
    color: #666;
  }

  .answers-list {
    max-height: 240px;
    overflow-y: auto;
  }

  .answer-row {
    padding: 12px 0;
    border-bottom: 1px solid #eee;

    &:last-child {
      border-bottom: none;
    }

    .answer-q {
      font-size: 13px;
      color: #666;
      margin-bottom: 4px;
    }

    .answer-a {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .answer-text {
        font-size: 14px;
        color: #1a1a1a;
      }

      .answer-score {
        font-size: 12px;
        padding: 2px 8px;
        border-radius: 4px;

        &.correct {
          background: #f0f9eb;
          color: #67c23a;
        }

        &.wrong {
          background: #fef0f0;
          color: #f56c6c;
        }
      }
    }
  }
}

.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

@media (max-width: 480px) {
  .success-card {
    padding: 24px;
  }

  .score-section {
    padding: 20px;

    .score-value {
      font-size: 36px;
    }
  }
}
</style>
