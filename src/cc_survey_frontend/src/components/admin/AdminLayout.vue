<template>
  <div class="admin-layout">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '240px'" class="admin-aside">
        <div class="logo">
          <div class="logo-icon">
            <el-icon :size="24"><DataAnalysis /></el-icon>
          </div>
          <transition name="fade">
            <span v-if="!isCollapse" class="logo-text">CC Survey</span>
          </transition>
        </div>

        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          router
          class="admin-menu"
        >
          <el-menu-item index="/admin/surveys">
            <el-icon><Document /></el-icon>
            <template #title>问卷管理</template>
          </el-menu-item>

          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>

          <el-menu-item index="/admin/answers">
            <el-icon><List /></el-icon>
            <template #title>答卷管理</template>
          </el-menu-item>

          <el-menu-item index="/admin/statistics">
            <el-icon><TrendCharts /></el-icon>
            <template #title>数据统计</template>
          </el-menu-item>

          <el-menu-item index="/admin/logs">
            <el-icon><Memo /></el-icon>
            <template #title>操作日志</template>
          </el-menu-item>

          <el-menu-item index="/admin/agent">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>AI 助手</template>
          </el-menu-item>
        </el-menu>

        <div class="sidebar-footer" v-if="!isCollapse">
          <div class="version">v1.0.0</div>
        </div>
      </el-aside>

      <!-- 主内容区 -->
      <el-container>
        <!-- 头部 -->
        <el-header class="admin-header">
          <div class="header-left">
            <el-button
              class="collapse-btn"
              circle
              @click="isCollapse = !isCollapse"
            >
              <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
            </el-button>
            <div class="breadcrumb">
              <span class="current-page">{{ currentPageTitle }}</span>
            </div>
          </div>

          <div class="header-right">
            <el-tooltip content="返回前台" placement="bottom">
              <el-button circle @click="goHome">
                <el-icon><House /></el-icon>
              </el-button>
            </el-tooltip>

            <el-dropdown @command="handleCommand" trigger="click">
              <div class="user-info">
                <el-avatar :size="36" class="user-avatar">
                  {{ authStore.name?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <div class="user-detail">
                  <span class="username">{{ authStore.name }}</span>
                  <span class="role">管理员</span>
                </div>
                <el-icon class="arrow"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="home">
                    <el-icon><House /></el-icon>
                    返回前台
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 内容 -->
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  DataAnalysis, Document, User, List, TrendCharts, Memo,
  Fold, Expand, House, ArrowDown, SwitchButton, ChatDotRound
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

const currentPageTitle = computed(() => {
  const titles = {
    '/admin/surveys': '问卷管理',
    '/admin/users': '用户管理',
    '/admin/answers': '答卷管理',
    '/admin/statistics': '数据统计',
    '/admin/logs': '操作日志',
    '/admin/agent': 'AI 助手'
  }
  return titles[route.path] || '管理后台'
})

function goHome() {
  router.push('/')
}

async function handleCommand(command) {
  if (command === 'home') {
    goHome()
  } else if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;
}

.admin-aside {
  background: linear-gradient(180deg, #1a1f36 0%, #252b48 100%);
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;

  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 0 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .logo-icon {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;
    }

    .logo-text {
      color: #fff;
      font-size: 18px;
      font-weight: 600;
      white-space: nowrap;
    }
  }

  .admin-menu {
    flex: 1;
    border-right: none;
    background: transparent;
    padding: 12px 0;

    :deep(.el-menu-item) {
      height: 50px;
      line-height: 50px;
      margin: 4px 12px;
      border-radius: 8px;
      color: rgba(255, 255, 255, 0.7);

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        color: #fff;
      }

      &.is-active {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
      }

      .el-icon {
        font-size: 18px;
      }
    }
  }

  .sidebar-footer {
    padding: 16px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);

    .version {
      text-align: center;
      color: rgba(255, 255, 255, 0.4);
      font-size: 12px;
    }
  }
}

.admin-header {
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      border: none;
      background: #f5f7fa;

      &:hover {
        background: #e4e7ed;
      }
    }

    .breadcrumb {
      .current-page {
        font-size: 16px;
        font-weight: 500;
        color: #303133;
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .el-button {
      border: none;
      background: #f5f7fa;

      &:hover {
        background: #e4e7ed;
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      padding: 6px 12px;
      border-radius: 8px;
      transition: background 0.2s;

      &:hover {
        background: #f5f7fa;
      }

      .user-avatar {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        font-weight: 600;
      }

      .user-detail {
        display: flex;
        flex-direction: column;

        .username {
          font-size: 14px;
          font-weight: 500;
          color: #303133;
        }

        .role {
          font-size: 12px;
          color: #909399;
        }
      }

      .arrow {
        color: #909399;
        font-size: 12px;
      }
    }
  }
}

.admin-main {
  background: #f5f7fa;
  padding: 0;
  overflow: hidden;
  height: calc(100vh - 64px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>