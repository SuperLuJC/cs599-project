import api from './index'

/**
 * 认证API
 */
export const authApi = {
  /**
   * 用户登录
   */
  login(data) {
    return api.post('/auth/login', data)
  },

  /**
   * 用户注册
   */
  register(data) {
    return api.post('/auth/register', data)
  },

  /**
   * 用户登出
   */
  logout() {
    return api.post('/auth/logout')
  },

  /**
   * 刷新Token
   */
  refreshToken(refreshToken) {
    return api.post('/auth/refresh', { refreshToken })
  },

  /**
   * 获取当前用户信息
   */
  getCurrentUser() {
    return api.get('/auth/me')
  },

  /**
   * 验证邮箱
   */
  verifyEmail(token) {
    return api.get('/auth/verify-email', { params: { token } })
  },

  /**
   * 请求密码重置
   */
  requestPasswordReset(email) {
    return api.post('/auth/password/reset-request', null, { params: { email } })
  },

  /**
   * 重置密码
   */
  resetPassword(token, newPassword) {
    return api.post('/auth/password/reset', null, {
      params: { token, newPassword }
    })
  }
}