<template>
  <div class="forgot-page">
    <div class="forgot-container">
      <!-- 左侧装饰 -->
      <div class="forgot-banner">
        <div class="banner-content">
          <h1>CC Survey</h1>
          <p>智能问卷系统</p>
          <div class="features">
            <div class="feature-item">
              <el-icon><Lock /></el-icon>
              <span>安全密码重置</span>
            </div>
            <div class="feature-item">
              <el-icon><Message /></el-icon>
              <span>邮件验证身份</span>
            </div>
            <div class="feature-item">
              <el-icon><Key /></el-icon>
              <span>快速找回账号</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单 -->
      <div class="forgot-form-wrapper">
        <div class="forgot-form">
          <h2>重置密码</h2>
          <p class="subtitle">输入注册邮箱，我们将发送重置链接</p>

          <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleSubmit">
            <el-form-item prop="email">
              <el-input
                v-model="form.email"
                placeholder="请输入注册邮箱"
                size="large"
                :prefix-icon="Message"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleSubmit"
                class="forgot-btn"
              >
                发送重置链接
              </el-button>
            </el-form-item>
          </el-form>

          <div class="forgot-footer">
            <router-link to="/login">
              <el-icon><ArrowLeft /></el-icon>
              返回登录
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Message, Key, ArrowLeft } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'

const formRef = ref()
const loading = ref(false)

const form = reactive({
  email: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.requestPasswordReset(form.email)
    ElMessage.success('如果邮箱存在，重置邮件已发送')
  } catch (error) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.forgot-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow-y: auto;
  padding: 20px;
}

.forgot-container {
  display: flex;
  width: 900px;
  height: 500px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.forgot-banner {
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

.forgot-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.forgot-form {
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

  .forgot-btn {
    width: 100%;
    height: 44px;
    font-size: 16px;
  }

  .forgot-footer {
    text-align: center;
    margin-top: 20px;

    a {
      color: #409eff;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 14px;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

@media (max-width: 768px) {
  .forgot-container {
    width: 100%;
    height: auto;
    flex-direction: column;
  }

  .forgot-banner {
    display: none;
  }
}
</style>