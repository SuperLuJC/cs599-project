import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true // 允许携带Cookie
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 添加追踪ID
    config.headers['X-Trace-ID'] = generateTraceId()
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data

    if (code === 200) {
      return response.data
    }

    // 处理业务错误
    if (code === 401) {
      // 未授权，跳转登录
      const authStore = useAuthStore()
      authStore.clearAuth()
      window.location.href = '/login'
      return Promise.reject(new Error(message || '未授权'))
    }

    if (code === 403) {
      // 权限不足
      ElMessage.error(message || '权限不足')
      return Promise.reject(new Error(message || '权限不足'))
    }

    // 其他业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    // 处理HTTP错误
    if (error.response) {
      const { status, data } = error.response
      let message = data?.message || '网络错误'

      switch (status) {
        case 400:
          message = data?.message || '请求参数错误'
          break
        case 401:
          message = '未授权，请重新登录'
          const authStore = useAuthStore()
          authStore.clearAuth()
          window.location.href = '/login'
          break
        case 403:
          message = '权限不足'
          break
        case 404:
          message = '资源不存在'
          break
        case 429:
          message = '请求过于频繁，请稍后再试'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = `请求失败 (${status})`
      }

      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }

    // 网络错误
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }

    return Promise.reject(error)
  }
)

/**
 * 生成追踪ID
 */
function generateTraceId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2, 9)
}

export default api