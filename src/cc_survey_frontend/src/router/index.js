import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 路由配置
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/components/auth/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/components/auth/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/components/auth/ForgotPassword.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    name: 'SurveyList',
    component: () => import('@/components/survey/SurveyList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/survey/:id',
    name: 'SurveyForm',
    component: () => import('@/components/survey/SurveyForm.vue'),
    meta: { public: true }
  },
  {
    path: '/survey/:id/success',
    name: 'SubmitSuccess',
    component: () => import('@/components/survey/SubmitSuccess.vue'),
    meta: { public: true }
  },
  {
    path: '/admin',
    component: () => import('@/components/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: '/admin/surveys',
    children: [
      {
        path: 'surveys',
        name: 'SurveyManage',
        component: () => import('@/components/admin/SurveyManage.vue')
      },
      {
        path: 'surveys/create',
        name: 'SurveyCreate',
        component: () => import('@/components/survey/SurveyCreate.vue')
      },
      {
        path: 'surveys/:id/edit',
        name: 'SurveyEdit',
        component: () => import('@/components/survey/SurveyCreate.vue')
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/components/admin/UserManage.vue')
      },
      {
        path: 'answers',
        name: 'AnswerManage',
        component: () => import('@/components/admin/AnswerManage.vue')
      },
      {
        path: 'logs',
        name: 'LogManage',
        component: () => import('@/components/admin/LogManage.vue')
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/components/admin/StatisticsDashboard.vue')
      },
      {
        path: 'agent',
        name: 'AgentChat',
        component: () => import('@/components/admin/AgentChat.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/components/common/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 如果还没有用户信息，尝试获取
  if (!authStore.user && to.meta.requiresAuth) {
    await authStore.fetchCurrentUser()
  }

  // 需要认证的路由
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // 需要管理员权限的路由
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return next({ name: 'SurveyList' })
  }

  // 已登录用户访问登录/注册页面
  if (to.meta.guest && authStore.isAuthenticated) {
    // 管理员跳转到后台，普通用户跳转到首页
    return next(authStore.isAdmin ? '/admin/surveys' : '/')
  }

  next()
})

export default router