<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧装饰 -->
      <div class="login-banner">
        <div class="banner-content">
          <h1>CC Survey</h1>
          <p>智能问卷系统</p>
          <div class="features">
            <div class="feature-item">
              <el-icon><Edit /></el-icon>
              <span>可视化问卷编辑</span>
            </div>
            <div class="feature-item">
              <el-icon><DataAnalysis /></el-icon>
              <span>数据统计分析</span>
            </div>
            <div class="feature-item">
              <el-icon><Share /></el-icon>
              <span>一键发布分享</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-wrapper">
        <div class="login-form">
          <h2>欢迎登录</h2>
          <p class="subtitle">请输入您的账号信息</p>

          <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="用户名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleLogin"
                class="login-btn"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="login-footer">
            <span>还没有账号？</span>
            <router-link to="/register">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Edit, DataAnalysis, Share } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const formRef = ref()
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')

    const redirect = route.query.redirect
    if (authStore.isAdmin) {
      router.push(redirect || '/admin/surveys')
    } else {
      router.push(redirect || '/')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow-y: auto;
  padding: 20px;
}

.login-container {
  display: flex;
  width: 900px;
  height: 500px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-banner {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;

  .banner-content {
    text-align: center;
    color: #fff;

    h1 {
      font-size: 42px;
      margin: 0 0 10px;
      font-weight: 700;
    }

    p {
      font-size: 18px;
      opacity: 0.9;
      margin: 0 0 40px;
    }

    .features {
      display: flex;
      flex-direction: column;
      gap: 20px;

      .feature-item {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 16px;
        opacity: 0.9;

        .el-icon {
          font-size: 24px;
        }
      }
    }
  }
}

.login-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-form {
  width: 100%;
  max-width: 300px;

  h2 {
    margin: 0 0 8px;
    font-size: 28px;
    color: #303133;
  }

  .subtitle {
    margin: 0 0 30px;
    color: #909399;
    font-size: 14px;
  }

  .login-btn {
    width: 100%;
    height: 44px;
    font-size: 16px;
  }

  .login-footer {
    text-align: center;
    margin-top: 20px;
    color: #909399;
    font-size: 14px;

    a {
      color: #409eff;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

@media (max-width: 768px) {
  .login-container {
    width: 100%;
    height: auto;
    flex-direction: column;
  }

  .login-banner {
    display: none;
  }
}
</style>