<template>
  <div class="not-found">
    <div class="content">
      <h1>404</h1>
      <p>页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
}

.content {
  text-align: center;
}

h1 {
  font-size: 120px;
  color: #409eff;
  margin: 0;
}

p {
  font-size: 24px;
  color: #606266;
  margin: 20px 0 40px;
}
</style>