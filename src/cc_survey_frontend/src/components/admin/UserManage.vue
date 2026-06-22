<template>
  <div class="user-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>用户管理</h2>
        <span class="total-count">共 {{ total }} 位用户</span>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建用户
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名/姓名/邮箱"
        clearable
        style="width: 280px"
        @keyup.enter="loadData"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="loadData">搜索</el-button>
        </template>
      </el-input>

      <el-select v-model="roleFilter" placeholder="角色筛选" clearable @change="loadData" style="width: 140px">
        <el-option label="管理员" value="admin" />
        <el-option label="普通用户" value="user" />
      </el-select>
    </div>

    <!-- 用户列表 -->
    <div class="user-list" v-loading="loading">
      <el-table :data="tableData" class="user-table">
        <el-table-column prop="username" label="用户名" width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" class="user-avatar">
                {{ row.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'" effect="light">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.lastLoginTime) || '从未登录' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button link type="warning" @click="handleResetPassword(row)">
                <el-icon><Key /></el-icon>
              </el-button>
              <el-button link :type="row.status === 1 ? 'info' : 'success'" @click="handleToggleStatus(row)">
                <el-icon><Switch /></el-icon>
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @change="loadData"
      class="pagination"
    />

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新建用户'" width="500px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="!isEdit">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Key, Switch, Delete } from '@element-plus/icons-vue'
import api from '@/api/index'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const roleFilter = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({
  uuid: '',
  username: '',
  name: '',
  email: '',
  password: '',
  role: 'user'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await api.get('/admin/users', {
      params: { page: page.value, size: size.value, keyword: keyword.value, role: roleFilter.value }
    })
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  form.uuid = ''
  form.username = ''
  form.name = ''
  form.email = ''
  form.password = ''
  form.role = 'user'
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.uuid = row.uuid
  form.username = row.username
  form.name = row.name
  form.email = row.email
  form.role = row.role
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await api.put(`/admin/users/${form.uuid}`, form)
      ElMessage.success('更新成功')
    } else {
      await api.post('/admin/users', form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleResetPassword(row) {
  const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', {
    inputPattern: /^.{8,}$/,
    inputErrorMessage: '密码长度不能少于8位'
  })
  await api.post(`/admin/users/${row.uuid}/reset-password`, null, { params: { newPassword: value } })
  ElMessage.success('密码重置成功')
}

async function handleToggleStatus(row) {
  await api.post(`/admin/users/${row.uuid}/toggle-status`)
  ElMessage.success('状态切换成功')
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定要删除该用户吗？', '警告', { type: 'warning' })
  await api.delete(`/admin/users/${row.uuid}`)
  ElMessage.success('删除成功')
  loadData()
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleString()
}
</script>

<style scoped lang="scss">
.user-manage {
  padding: 24px;
  height: 100%;
  overflow-y: auto;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .header-left {
      display: flex;
      align-items: baseline;
      gap: 15px;

      h2 {
        margin: 0;
        font-size: 22px;
        color: #303133;
      }

      .total-count {
        color: #909399;
        font-size: 14px;
      }
    }
  }

  .filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
  }

  .user-list {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .user-table {
    .user-cell {
      display: flex;
      align-items: center;
      gap: 10px;

      .user-avatar {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        font-weight: 600;
        flex-shrink: 0;
      }
    }

    .time-text {
      color: #909399;
      font-size: 13px;
    }

    .action-btns {
      display: flex;
      gap: 4px;
    }
  }

  .pagination {
    margin-top: 20px;
    padding-bottom: 20px;
    justify-content: center;
  }
}
</style>