<template>
  <div class="survey-create-page">
    <div class="page-header">
      <el-button link @click="handleBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>{{ isEdit ? '编辑问卷' : '创建问卷' }}</h2>
    </div>

    <SurveyEditor
      ref="editorRef"
      :initial-data="initialData"
      @save="handleSave"
      @publish="handlePublish"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import SurveyEditor from './SurveyEditor.vue'
import api from '@/api/index'

const route = useRoute()
const router = useRouter()

const editorRef = ref()
const initialData = ref(null)

const isEdit = computed(() => !!route.params.id)

onMounted(async () => {
  if (isEdit.value) {
    await loadSurvey()
  }
})

async function loadSurvey() {
  try {
    const res = await api.get(`/admin/surveys/${route.params.id}`)
    initialData.value = res.data
  } catch (error) {
    ElMessage.error('加载问卷失败')
    router.back()
  }
}

async function handleSave(data) {
  try {
    if (isEdit.value) {
      await api.put(`/admin/surveys/${route.params.id}`, data)
      ElMessage.success('保存成功')
    } else {
      await api.post('/admin/surveys', data)
      ElMessage.success('保存成功')
      router.push('/admin/surveys')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}

async function handlePublish(data) {
  try {
    let uuid
    if (isEdit.value) {
      await api.put(`/admin/surveys/${route.params.id}`, data)
      uuid = route.params.id
    } else {
      const res = await api.post('/admin/surveys', data)
      uuid = res.data.uuid
    }

    await api.post(`/admin/surveys/${uuid}/publish`)
    ElMessage.success('发布成功')
    router.push('/admin/surveys')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '发布失败')
  }
}

function handleBack() {
  router.back()
}
</script>

<style scoped lang="scss">
.survey-create-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .page-header {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 15px 20px;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
    flex-shrink: 0;
    z-index: 10;

    h2 {
      margin: 0;
      font-size: 18px;
    }
  }
}
</style>