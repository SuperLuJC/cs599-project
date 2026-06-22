<template>
  <div class="register-page">
    <div class="register-container">
      <!-- 左侧装饰 -->
      <div class="register-banner">
        <div class="banner-content">
          <h1>CC Survey</h1>
          <p>智能问卷系统</p>
          <div class="features">
            <div class="feature-item">
              <el-icon><UserFilled /></el-icon>
              <span>快速注册账号</span>
            </div>
            <div class="feature-item">
              <el-icon><Message /></el-icon>
              <span>邮箱安全验证</span>
            </div>
            <div class="feature-item">
              <el-icon><Lock /></el-icon>
              <span>数据安全保障</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧注册表单 -->
      <div class="register-form-wrapper">
        <div class="register-form">
          <h2>创建账号</h2>
          <p class="subtitle">填写以下信息完成注册</p>

          <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleRegister">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="用户名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="email">
              <el-input
                v-model="form.email"
                placeholder="邮箱地址"
                size="large"
                :prefix-icon="Message"
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

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="确认密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item prop="name">
              <el-input
                v-model="form.name"
                placeholder="真实姓名（必填）"
                size="large"
                :prefix-icon="UserFilled"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleRegister"
                class="register-btn"
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="register-footer">
            <span>已有账号？</span>
            <router-link to="/login">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled, Message } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  name: ''
})

const validatePassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '用户名必须以字母开头', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/, message: '密码必须包含大小写字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度为2-50个字符', trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.register({
      username: form.username,
      email: form.email,
      password: form.password,
      name: form.name
    })

    ElMessage.success('注册成功，请查收验证邮件')
    router.push('/login')
  } catch (error) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow-y: auto;
  padding: 20px;
}

.register-container {
  display: flex;
  width: 900px;
  height: auto;
  min-height: 580px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.register-banner {
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

.register-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.register-form {
  width: 100%;
  max-width: 320px;

  h2 {
    margin: 0 0 8px;
    font-size: 28px;
    color: #303133;
  }

  .subtitle {
    margin: 0 0 25px;
    color: #909399;
    font-size: 14px;
  }

  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  .register-btn {
    width: 100%;
    height: 44px;
    font-size: 16px;
    margin-top: 5px;
  }

  .register-footer {
    text-align: center;
    margin-top: 20px;
    color: #909399;
    font-size: 14px;

    a {
      color: #409eff;
      text-decoration: none;
      margin-left: 5px;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

@media (max-width: 768px) {
  .register-container {
    width: 100%;
    height: auto;
    flex-direction: column;
  }

  .register-banner {
    display: none;
  }
}
</style>