import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref(null)
  const loading = ref(false)

  // 计算属性
  const isAuthenticated = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')
  const username = computed(() => user.value?.username || '')
  const name = computed(() => user.value?.name || user.value?.username || '')

  /**
   * 登录
   */
  async function login(credentials) {
    loading.value = true
    try {
      const response = await authApi.login(credentials)
      user.value = response.data
      return response
    } finally {
      loading.value = false
    }
  }

  /**
   * 注册
   */
  async function register(data) {
    loading.value = true
    try {
      return await authApi.register(data)
    } finally {
      loading.value = false
    }
  }

  /**
   * 登出
   */
  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {
      // 忽略登出错误
    } finally {
      clearAuth()
    }
  }

  /**
   * 获取当前用户信息
   */
  async function fetchCurrentUser() {
    try {
      const response = await authApi.getCurrentUser()
      user.value = response.data
      return response.data
    } catch (e) {
      clearAuth()
      return null
    }
  }

  /**
   * 清除认证信息
   */
  function clearAuth() {
    user.value = null
  }

  return {
    // 状态
    user,
    loading,
    // 计算属性
    isAuthenticated,
    isAdmin,
    username,
    name,
    // 方法
    login,
    register,
    logout,
    fetchCurrentUser,
    clearAuth
  }
})