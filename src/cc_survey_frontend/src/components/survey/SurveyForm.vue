<template>
  <div class="survey-form-page">
    <!-- 进度条和返回按钮 -->
    <div class="progress-header" v-if="survey">
      <div class="progress-top">
        <el-button size="small" @click="handleBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <div class="progress-info">
          <span>填写进度</span>
          <span class="progress-count">{{ filledCount }} / {{ totalFillableFields }}</span>
        </div>
      </div>
      <el-progress
        :percentage="progressPercent"
        :stroke-width="6"
        :show-text="false"
        color="#409eff"
      />
    </div>

    <!-- 问卷内容 -->
    <div class="form-container" v-loading="loading">
      <template v-if="survey">
        <!-- 问卷标题 -->
        <div class="form-title-section">
          <h1>{{ survey.title }}</h1>
          <p v-if="survey.description">{{ survey.description }}</p>

          <!-- 创建人信息 -->
          <div class="creator-info" v-if="survey.createdByName">
            <el-icon><User /></el-icon>
            <span>创建人：{{ survey.createdByName }}</span>
          </div>

          <!-- 显示剩余份数 -->
          <div v-if="quotaInfo.limited" class="quota-info">
            <el-tag :type="quotaInfo.remaining > 0 ? 'success' : 'danger'" size="small">
              剩余份数: {{ quotaInfo.remaining }} / {{ quotaInfo.maxSubmissions }}
            </el-tag>
            <span v-if="quotaInfo.remaining === 0" class="quota-warning">
              (已满，无法提交)
            </span>
          </div>

          <!-- 已提交提示 -->
          <div v-if="hasSubmitted" class="submitted-info">
            <el-tag type="success" size="small">您已提交过此问卷</el-tag>
            <span class="edit-hint" v-if="survey.allowEdit">（可修改重新提交）</span>
          </div>
        </div>

        <!-- 未开始提示 -->
        <div v-if="notStartedInfo.isNotStarted" class="not-started-banner">
          <div class="countdown-wrapper">
            <el-icon class="warning-icon"><Clock /></el-icon>
            <div class="countdown-content">
              <p class="countdown-title">问卷尚未开始</p>
              <p class="countdown-time">开始时间：{{ formatTime(survey.startTime) }}</p>
              <p class="countdown-remaining">
                距离开始还有：<span class="countdown-timer">{{ notStartedInfo.countdown }}</span>
              </p>
            </div>
          </div>
        </div>

        <!-- 已结束提示 -->
        <div v-if="isExpired" class="expired-banner">
          <el-icon class="warning-icon"><WarningFilled /></el-icon>
          <span>问卷已结束，感谢您的关注！</span>
        </div>

        <!-- 表单 -->
        <div class="form-body" :class="{ 'form-disabled': notStartedInfo.isNotStarted || isExpired }">
          <template v-for="(field, index) in fields" :key="field.id || field.name">
            <div
              v-if="isFieldVisible(field)"
              class="field-block"
              :class="{ filled: isFieldFilled(field) }"
            >
              <!-- 题号和标题 -->
              <div class="field-header">
                <span class="field-number">{{ index + 1 }}</span>
                <div class="field-title">
                  <span class="title-text">{{ field.label }}</span>
                  <span v-if="field.required" class="required-mark">必填</span>
                  <span v-if="field.computeRule || field.autoCalculate" class="auto-mark">自动计算</span>
                </div>
              </div>

              <!-- 题目提示 -->
              <div v-if="field.hint" class="field-hint" v-html="field.hint"></div>

              <!-- 多字段组合输入 -->
              <template v-if="field.type === 'multi-input'">
                <div class="multi-input-group">
                  <div v-for="subField in field.subFields" :key="subField.id" class="sub-field-item">
                    <label class="sub-label">{{ subField.label }}</label>
                    <div v-if="subField.hint" class="sub-hint">{{ subField.hint }}</div>
                    <template v-if="subField.type === 'input' || !subField.type">
                      <el-input
                        v-model="formData[subField.id]"
                        :placeholder="subField.placeholder || '请输入'"
                        :readonly="subField.readonly"
                        :type="subField.inputType === 'number' ? 'number' : 'text'"
                        size="default"
                        @blur="validateSubField(field, subField)"
                      />
                    </template>
                    <template v-else-if="subField.type === 'upload'">
                      <el-upload
                        :action="getUploadAction(subField)"
                        :on-success="(res, file) => handleSubUploadSuccess(subField.id, res, file)"
                        :file-list="fileLists[subField.id] || []"
                        :limit="1"
                        :accept="subField.accept"
                        drag
                      >
                        <el-button type="primary" size="default">{{ subField.btnText || '点击上传' }}</el-button>
                      </el-upload>
                    </template>
                    <div v-if="subFieldErrors[subField.id]" class="field-error-text">
                      {{ subFieldErrors[subField.id] }}
                    </div>
                  </div>
                </div>
              </template>

              <!-- 自动计算/只读字段 -->
              <template v-else-if="field.computeRule || field.autoCalculate || field.readonly">
                <div class="computed-value">
                  {{ getComputedValue(field) }}
                </div>
              </template>

              <!-- 文本输入 -->
              <template v-else-if="field.type === 'input'">
                <el-input
                  v-model="formData[field.id || field.name]"
                  :placeholder="field.placeholder || '请输入'"
                  :type="field.inputType === 'number' ? 'number' : 'text'"
                  size="large"
                  @blur="validateField(field)"
                />
                <div v-if="field.errorMessage && fieldErrors[field.id || field.name]" class="field-error-text">
                  {{ field.errorMessage }}
                </div>
              </template>

              <!-- 单选题 -->
              <template v-else-if="field.type === 'radio'">
                <div class="options-list">
                  <label
                    v-for="(opt, optIndex) in field.options"
                    :key="opt.value"
                    class="option-item"
                    :class="{ selected: formData[field.id || field.name] === opt.value }"
                  >
                    <input
                      type="radio"
                      :name="field.id || field.name"
                      :value="opt.value"
                      v-model="formData[field.id || field.name]"
                      :disabled="field.readonly"
                    />
                    <span class="option-marker"></span>
                    <span class="option-content">
                      <span class="option-label">
                        <span class="option-prefix">{{ getOptionLabel(optIndex) }}.</span>
                        {{ opt.label }}
                      </span>
                      <span v-if="opt.hint" class="option-hint">{{ opt.hint }}</span>
                    </span>
                  </label>
                </div>
              </template>

              <!-- 多选题 -->
              <template v-else-if="field.type === 'checkbox'">
                <div class="options-list">
                  <label
                    v-for="(opt, optIndex) in field.options"
                    :key="opt.value"
                    class="option-item checkbox"
                    :class="{ selected: formData[field.id || field.name]?.includes(opt.value) }"
                  >
                    <input
                      type="checkbox"
                      :value="opt.value"
                      v-model="formData[field.id || field.name]"
                    />
                    <span class="option-marker"></span>
                    <span class="option-content">
                      <span class="option-label">
                        <span class="option-prefix">{{ getOptionLabel(optIndex) }}.</span>
                        {{ opt.label }}
                      </span>
                      <span v-if="opt.hint" class="option-hint">{{ opt.hint }}</span>
                    </span>
                  </label>
                </div>
              </template>

              <!-- 日期选择 -->
              <template v-else-if="field.type === 'date'">
                <el-date-picker
                  v-model="formData[field.id || field.name]"
                  :type="field.dateType || 'date'"
                  :placeholder="field.placeholder || '请选择日期'"
                  :format="field.format || 'YYYY-MM-DD'"
                  size="large"
                  style="width: 100%"
                />
              </template>

              <!-- 文件上传 -->
              <template v-else-if="field.type === 'upload'">
                <el-upload
                  :action="getUploadAction(field)"
                  :on-success="(res, file) => handleUploadSuccess(field.id || field.name, res, file)"
                  :file-list="fileLists[field.id || field.name] || []"
                  :limit="field.maxFiles || 1"
                  :accept="field.accept"
                  drag
                  class="upload-area"
                >
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <div class="upload-text">{{ field.btnText || '点击或拖拽文件到此处上传' }}</div>
                </el-upload>
              </template>

              <!-- 默认输入 -->
              <template v-else>
                <el-input
                  v-model="formData[field.id || field.name]"
                  :placeholder="field.placeholder || '请输入'"
                  size="large"
                />
              </template>

              <!-- 额外输入 -->
              <template v-if="field.extraInput && shouldShowExtraInput(field)">
                <div class="extra-input-section">
                  <label class="extra-label">{{ field.extraInput.hint || '请补充信息' }}</label>
                  <template v-if="field.extraInput.inputType === 'text' || field.extraInput.type === 'input'">
                    <el-input
                      v-model="formData[field.extraInput.id]"
                      :placeholder="field.extraInput.placeholder || '请输入'"
                      size="large"
                    />
                  </template>
                  <template v-else-if="field.extraInput.inputType === 'number'">
                    <el-input
                      v-model="formData[field.extraInput.id]"
                      type="number"
                      :placeholder="field.extraInput.placeholder || '请输入'"
                      size="large"
                    />
                  </template>
                  <template v-else-if="field.extraInput.type === 'upload'">
                    <el-upload
                      :action="getUploadAction(field.extraInput)"
                      :on-success="(res, file) => handleUploadSuccess(field.extraInput.id, res, file)"
                      :file-list="fileLists[field.extraInput.id] || []"
                      :limit="1"
                      :accept="field.extraInput.accept"
                      drag
                    >
                      <el-button type="primary">{{ field.extraInput.btnText || '点击上传' }}</el-button>
                    </el-upload>
                  </template>
                  <div v-if="field.extraInput.errorMessage && !formData[field.extraInput.id]" class="field-error-text">
                    {{ field.extraInput.errorMessage }}
                  </div>
                </div>
              </template>
            </div>
          </template>
        </div>

        <!-- 底部操作 -->
        <div class="form-footer">
          <el-button
            type="primary"
            size="large"
            @click="handleSubmit"
            :loading="submitting"
            :disabled="notStartedInfo.isNotStarted || isExpired"
            class="submit-btn"
          >
            <template v-if="notStartedInfo.isNotStarted">
              未开始
            </template>
            <template v-else-if="isExpired">
              已结束
            </template>
            <template v-else>
              {{ hasSubmitted ? '修改提交' : '提交问卷' }}
            </template>
          </el-button>
        </div>
      </template>

      <div v-else-if="!loading" class="not-found">
        <el-icon :size="64" color="#c0c4cc"><Document /></el-icon>
        <p>问卷不存在或已下线</p>
        <el-button type="primary" @click="handleBack">返回列表</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, UploadFilled, Clock, WarningFilled, ArrowLeft, User } from '@element-plus/icons-vue'
import api from '@/api/index'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const survey = ref(null)
const hasSubmitted = ref(false)
const quotaInfo = ref({
  limited: false,
  maxSubmissions: 0,
  submittedCount: 0,
  remaining: -1
})
const formRef = ref()
const formData = reactive({})
const formRules = reactive({})
const fileLists = reactive({})
const fieldErrors = reactive({})
const subFieldErrors = reactive({})

// 倒计时相关
const notStartedInfo = ref({
  isNotStarted: false,
  countdown: '',
  startTime: null
})
let countdownTimer = null

const surveyId = computed(() => route.params.id)

// 是否已过期
const isExpired = computed(() => {
  if (!survey.value?.endTime) return false
  return new Date(survey.value.endTime).getTime() < Date.now()
})

const fields = computed(() => {
  if (survey.value?.schemaJson?.fields) return survey.value.schemaJson.fields
  if (survey.value?.fields) return survey.value.fields
  return []
})

const totalFillableFields = computed(() => {
  let count = 0
  fields.value.forEach(field => {
    if (field.computeRule || field.autoCalculate || field.readonly) return
    if (!isFieldVisible(field)) return
    if (field.type === 'multi-input' && field.subFields) {
      count += field.subFields.filter(sf => !sf.readonly && !sf.computeRule).length
    } else {
      count++
    }
  })
  return count
})

const filledCount = computed(() => {
  let count = 0
  fields.value.forEach(field => {
    if (field.computeRule || field.autoCalculate || field.readonly) return
    if (!isFieldVisible(field)) return
    if (field.type === 'multi-input' && field.subFields) {
      field.subFields.forEach(sf => {
        if (!sf.readonly && !sf.computeRule && isSubFieldFilled(sf)) count++
      })
    } else if (isFieldFilled(field)) {
      count++
    }
  })
  return count
})

const progressPercent = computed(() => {
  if (totalFillableFields.value === 0) return 0
  return Math.round((filledCount.value / totalFillableFields.value) * 100)
})

watch(formData, () => {
  updateComputedFields()
}, { deep: true })

onMounted(() => {
  loadSurvey()
})

onUnmounted(() => {
  // 清除倒计时定时器
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})

async function loadSurvey() {
  loading.value = true
  try {
    const res = await api.get(`/surveys/${surveyId.value}`)
    console.log('survey response:', res.data)
    survey.value = res.data
    initForm()
    // 加载配额信息
    loadQuota()
    // 加载用户已提交的答案
    await loadUserAnswer()
    // 初始化倒计时
    initCountdown()
  } catch (error) {
    console.error('加载问卷失败:', error.response?.status, error.response?.data)
    if (error.response?.status === 404) {
      ElMessage.error('问卷不存在')
    } else {
      ElMessage.error('加载问卷失败: ' + (error.response?.data?.message || '服务器错误'))
    }
  } finally {
    loading.value = false
  }
}

async function loadUserAnswer() {
  try {
    const res = await api.get(`/surveys/${surveyId.value}/my-answer`)
    console.log('my-answer response:', res.data)
    if (res.data) {
      hasSubmitted.value = true
      // 回显答案数据
      const answerData = res.data.answerData || {}
      console.log('answerData:', answerData)
      Object.keys(answerData).forEach(key => {
        formData[key] = answerData[key]
      })
      // 处理文件列表回显
      fields.value.forEach(field => {
        if (field.type === 'upload' && formData[field.id]) {
          const fileUrl = formData[field.id]
          fileLists[field.id] = [{
            name: fileUrl.split('/').pop() || '已上传文件',
            url: fileUrl
          }]
        }
      })
    }
  } catch (error) {
    // 未提交或其他错误，记录日志
    console.warn('加载用户答案失败:', error.response?.status, error.response?.data)
  }
}

async function loadQuota() {
  try {
    const res = await api.get(`/surveys/${surveyId.value}/quota`)
    quotaInfo.value = res.data
  } catch (error) {
    // 配额信息加载失败不影响问卷显示
    console.warn('Failed to load quota info:', error)
  }
}

// 初始化倒计时
function initCountdown() {
  if (!survey.value?.startTime) {
    notStartedInfo.value.isNotStarted = false
    return
  }

  const startTime = new Date(survey.value.startTime).getTime()
  const now = Date.now()

  if (startTime > now) {
    notStartedInfo.value.isNotStarted = true
    notStartedInfo.value.startTime = startTime
    updateCountdown()

    // 每秒更新倒计时
    countdownTimer = setInterval(() => {
      updateCountdown()
    }, 1000)
  } else {
    notStartedInfo.value.isNotStarted = false
  }
}

// 更新倒计时显示
function updateCountdown() {
  const now = Date.now()
  const startTime = notStartedInfo.value.startTime

  if (!startTime) return

  const diff = startTime - now

  if (diff <= 0) {
    // 时间到了，解锁表单
    notStartedInfo.value.isNotStarted = false
    notStartedInfo.value.countdown = ''
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
    ElMessage.success('问卷已开始，可以填写了！')
    return
  }

  // 计算天、时、分、秒
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)

  let countdownStr = ''
  if (days > 0) {
    countdownStr = `${days}天 ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  } else if (hours > 0) {
    countdownStr = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  } else if (minutes > 0) {
    countdownStr = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  } else {
    countdownStr = `${seconds}秒`
  }

  notStartedInfo.value.countdown = countdownStr
}

// 格式化时间显示
function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const year = date.getFullYear()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  const seconds = date.getSeconds().toString().padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

function initForm() {
  fields.value.forEach(field => {
    const fieldId = field.id || field.name
    if (field.type === 'checkbox') {
      formData[fieldId] = []
    } else if (field.type === 'multi-input' && field.subFields) {
      field.subFields.forEach(subField => {
        if (subField.type === 'upload') {
          fileLists[subField.id] = []
          formData[subField.id] = ''
        } else {
          formData[subField.id] = subField.readonly ? '' : (subField.defaultValue || '')
        }
      })
    } else if (field.type === 'upload') {
      fileLists[fieldId] = []
      formData[fieldId] = ''
    } else {
      formData[fieldId] = field.defaultValue || ''
    }
    if (field.extraInput) {
      if (field.extraInput.type === 'upload') {
        fileLists[field.extraInput.id] = []
        formData[field.extraInput.id] = ''
      } else {
        formData[field.extraInput.id] = ''
      }
    }
  })
  updateComputedFields()
}

function isFieldVisible(field) {
  if (!field.showWhen) return true
  try {
    return evaluateCondition(field.showWhen)
  } catch (e) {
    return true
  }
}

function evaluateCondition(expression) {
  try {
    let exp = expression
    Object.keys(formData).forEach(key => {
      const value = formData[key]
      if (Array.isArray(value)) {
        exp = exp.replace(new RegExp(`\\b${key}\\b`, 'g'), JSON.stringify(value))
      } else if (typeof value === 'string') {
        exp = exp.replace(new RegExp(`\\b${key}\\b`, 'g'), `'${value}'`)
      } else {
        exp = exp.replace(new RegExp(`\\b${key}\\b`, 'g'), value ?? 'null')
      }
    })
    return Function(`return ${exp}`)()
  } catch (e) {
    return true
  }
}

function shouldShowExtraInput(field) {
  if (!field.extraInput?.showWhen) {
    const value = formData[field.id || field.name]
    if (field.type === 'radio') {
      return value && value !== 'A' && value !== ''
    } else if (field.type === 'checkbox') {
      return value && value.length > 0 && !value.includes('A')
    }
    return false
  }
  return evaluateCondition(field.extraInput.showWhen)
}

function updateComputedFields() {
  fields.value.forEach(field => {
    const fieldId = field.id || field.name
    if (field.computeRule) {
      formData[fieldId] = evaluateComputeRule(field.computeRule)
    }
    if (field.type === 'multi-input' && field.subFields) {
      field.subFields.forEach(subField => {
        if (subField.computeRule) {
          formData[subField.id] = evaluateComputeRule(subField.computeRule)
        }
      })
    }
  })
}

function evaluateComputeRule(rule) {
  try {
    let expression = rule
    Object.keys(formData).forEach(key => {
      const value = formData[key]
      if (typeof value === 'string') {
        expression = expression.replace(new RegExp(`\\b${key}\\b`, 'g'), `Number('${value}')`)
      } else {
        expression = expression.replace(new RegExp(`\\b${key}\\b`, 'g'), value ?? 0)
      }
    })
    return Function(`return ${expression}`)()
  } catch (e) {
    return ''
  }
}

function getComputedValue(field) {
  const fieldId = field.id || field.name
  const value = formData[fieldId]
  if (value === undefined || value === null || value === '') return ''
  if (typeof value === 'number') {
    if (field.calculateFormat === 'percent') {
      return (value * 100).toFixed(1) + '%'
    }
    return Number.isInteger(value) ? value : value.toFixed(2)
  }
  return value
}

function isFieldFilled(field) {
  const fieldId = field.id || field.name
  const value = formData[fieldId]
  if (field.type === 'checkbox') return value && value.length > 0
  if (field.type === 'upload') return fileLists[fieldId] && fileLists[fieldId].length > 0
  return value !== '' && value !== null && value !== undefined
}

function isSubFieldFilled(subField) {
  const value = formData[subField.id]
  if (subField.type === 'upload') return fileLists[subField.id] && fileLists[subField.id].length > 0
  return value !== '' && value !== null && value !== undefined
}

function validateField(field) {
  const fieldId = field.id || field.name
  const value = formData[fieldId]
  if (field.pattern && value) {
    try {
      const regex = new RegExp(field.pattern)
      if (!regex.test(value)) {
        fieldErrors[fieldId] = true
        return false
      }
    } catch (e) {}
  }
  fieldErrors[fieldId] = false
  return true
}

function validateSubField(field, subField) {
  const value = formData[subField.id]
  if (subField.pattern && value) {
    try {
      const regex = new RegExp(subField.pattern)
      if (!regex.test(value)) {
        subFieldErrors[subField.id] = subField.errorMessage || '格式不正确'
        return false
      }
    } catch (e) {}
  }
  if (subField.validatorRule && subField.validatorRule.logic) {
    const valid = evaluateCondition(subField.validatorRule.logic)
    if (!valid) {
      subFieldErrors[subField.id] = subField.validatorRule.message || '验证失败'
      return false
    }
  }
  subFieldErrors[subField.id] = ''
  return true
}

function handleUploadSuccess(fieldId, res, file) {
  if (res.code === 200 || res.code === 0) {
    if (!fileLists[fieldId]) fileLists[fieldId] = []
    fileLists[fieldId].push({
      name: file.name,
      url: res.data?.url || res.url,
      uuid: res.data?.uuid || res.uuid
    })
    formData[fieldId] = res.data?.url || res.url
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

function handleSubUploadSuccess(fieldId, res, file) {
  handleUploadSuccess(fieldId, res, file)
}

function buildSubmitData() {
  const data = {}
  fields.value.forEach(field => {
    const fieldId = field.id || field.name
    if (field.type === 'multi-input' && field.subFields) {
      const subData = {}
      field.subFields.forEach(subField => {
        subData[subField.id] = formData[subField.id]
        if (subField.type === 'upload' && fileLists[subField.id]?.length > 0) {
          subData[subField.id] = fileLists[subField.id].map(f => f.url || f.uuid)
        }
      })
      data[fieldId] = subData
    } else if (field.type === 'upload') {
      data[fieldId] = fileLists[fieldId]?.map(f => f.url || f.uuid) || []
    } else {
      data[fieldId] = formData[fieldId]
    }
    if (field.extraInput && shouldShowExtraInput(field)) {
      const extraId = field.extraInput.id
      if (field.extraInput.type === 'upload') {
        data[extraId] = fileLists[extraId]?.map(f => f.url || f.uuid) || []
      } else {
        data[extraId] = formData[extraId]
      }
    }
  })
  return data
}

async function handleSubmit() {
  // 检查配额
  if (quotaInfo.value.limited && quotaInfo.value.remaining === 0) {
    ElMessage.error('问卷提交份数已达上限，感谢您的参与！')
    return
  }

  let valid = true
  fields.value.forEach(field => {
    if (field.type === 'multi-input' && field.subFields) {
      field.subFields.forEach(subField => {
        if (subField.required && !isSubFieldFilled(subField)) valid = false
        if (!validateSubField(field, subField)) valid = false
      })
    } else if (field.required && !isFieldFilled(field)) {
      valid = false
    }
  })
  if (!valid) {
    ElMessage.warning('请完成所有必填项')
    return
  }
  submitting.value = true
  try {
    const submitData = buildSubmitData()
    await api.post('/submit', {
      templateUuid: surveyId.value,
      data: submitData
    })
    router.push(`/survey/${surveyId.value}/success`)
  } catch (error) {
    const code = error.response?.data?.code
    const message = error.response?.data?.message
    if (code === 4006) {
      // SUBMISSION_LIMIT_REACHED
      ElMessage.error('问卷提交份数已达上限，感谢您的参与！')
      // 刷新配额信息
      loadQuota()
    } else {
      ElMessage.error(message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

function handleBack() {
  router.push('/')
}

function getOptionLabel(index) {
  const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  return letters[index] || String(index + 1)
}

function getUploadAction(field) {
  const baseUrl = field?.action || '/api/upload'
  const title = encodeURIComponent(survey.value?.title || 'survey')
  const separator = baseUrl.includes('?') ? '&' : '?'
  return `${baseUrl}${separator}surveyTitle=${title}`
}
</script>

<style scoped lang="scss">
.survey-form-page {
  min-height: 100vh;
  background: #f5f7fa;
}

// 顶部导航栏
.top-nav {
  position: sticky;
  top: 0;
  background: #fff;
  padding: 12px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  z-index: 101;
  display: flex;
  align-items: center;

  .back-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #606266;
    border: none;
    background: transparent;

    &:hover {
      color: #409eff;
      background: #f5f7fa;
    }
  }
}

.progress-header {
  position: sticky;
  top: 0;
  background: #fff;
  padding: 16px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  z-index: 100;

  .progress-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }

  .back-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #606266;
    border: none;
    background: transparent;

    &:hover {
      color: #409eff;
      background: #f5f7fa;
    }
  }

  .progress-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
    color: #666;

    .progress-count {
      font-weight: 600;
      color: #409eff;
    }
  }
}

.form-container {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px;
}

// 未开始提示横幅
.not-started-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  color: #fff;

  .countdown-wrapper {
    display: flex;
    align-items: center;
    gap: 16px;

    .warning-icon {
      font-size: 48px;
      opacity: 0.9;
    }

    .countdown-content {
      flex: 1;

      .countdown-title {
        font-size: 18px;
        font-weight: 600;
        margin: 0 0 8px;
      }

      .countdown-time {
        font-size: 14px;
        opacity: 0.9;
        margin: 0 0 8px;
      }

      .countdown-remaining {
        font-size: 14px;
        margin: 0;

        .countdown-timer {
          font-size: 20px;
          font-weight: 700;
          color: #ffd700;
          margin-left: 4px;
        }
      }
    }
  }
}

// 已结束提示横幅
.expired-banner {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 12px;
  padding: 16px 24px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #f56c6c;

  .warning-icon {
    font-size: 24px;
  }
}

// 表单禁用状态
.form-disabled {
  pointer-events: none;
  opacity: 0.6;

  // 但仍可查看内容
  * {
    cursor: not-allowed !important;
  }
}

.form-title-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  h1 {
    margin: 0 0 8px;
    font-size: 22px;
    font-weight: 600;
    color: #1a1a1a;
  }

  p {
    margin: 0;
    font-size: 14px;
    color: #666;
  }

  .creator-info {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #888;
  }

  .quota-info {
    margin-top: 12px;
    display: flex;
    align-items: center;
    gap: 8px;

    .quota-warning {
      color: #f56c6c;
      font-size: 13px;
    }
  }

  .submitted-info {
    margin-top: 12px;
    display: flex;
    align-items: center;
    gap: 8px;

    .edit-hint {
      font-size: 13px;
      color: #888;
    }
  }
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-block {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border-left: 3px solid transparent;
  transition: border-color 0.2s;

  &.filled {
    border-left-color: #67c23a;
  }
}

.field-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;

  .field-number {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    background: #409eff;
    color: #fff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
  }

  .field-title {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;

    .title-text {
      font-size: 16px;
      font-weight: 500;
      color: #1a1a1a;
      line-height: 1.5;
    }

    .required-mark {
      font-size: 12px;
      color: #f56c6c;
      background: #fef0f0;
      padding: 2px 6px;
      border-radius: 4px;
    }

    .auto-mark {
      font-size: 12px;
      color: #67c23a;
      background: #f0f9eb;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}

.field-hint {
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  line-height: 1.6;

  a {
    color: #409eff;
    text-decoration: underline;
  }
}

// 选项列表
.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  input {
    display: none;
  }

  .option-marker {
    flex-shrink: 0;
    width: 18px;
    height: 18px;
    border: 2px solid #d9d9d9;
    border-radius: 50%;
    margin-top: 2px;
    transition: all 0.2s;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%) scale(0);
      width: 8px;
      height: 8px;
      background: #409eff;
      border-radius: 50%;
      transition: transform 0.2s;
    }
  }

  &.checkbox .option-marker {
    border-radius: 4px;

    &::after {
      border-radius: 2px;
    }
  }

  &:hover {
    background: #f0f7ff;
    border-color: #409eff;

    .option-marker {
      border-color: #409eff;
    }
  }

  &.selected {
    background: #e6f4ff;
    border-color: #409eff;

    .option-marker {
      border-color: #409eff;

      &::after {
        transform: translate(-50%, -50%) scale(1);
      }
    }
  }

  .option-content {
    flex: 1;
    min-width: 0;
  }

  .option-label {
    display: block;
    font-size: 15px;
    color: #1a1a1a;
    line-height: 1.6;
    word-break: break-word;
  }

  .option-prefix {
    font-weight: 600;
    color: #409eff;
    margin-right: 4px;
  }

  .option-hint {
    display: block;
    margin-top: 6px;
    font-size: 13px;
    color: #888;
    line-height: 1.5;
  }
}

// 多字段组合
.multi-input-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sub-field-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;

  .sub-label {
    display: block;
    font-size: 14px;
    font-weight: 500;
    color: #1a1a1a;
    margin-bottom: 8px;
  }

  .sub-hint {
    font-size: 12px;
    color: #888;
    margin-bottom: 8px;
  }
}

// 计算值
.computed-value {
  padding: 16px;
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
  border-radius: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #67c23a;
  text-align: center;
}

// 额外输入
.extra-input-section {
  margin-top: 16px;
  padding: 16px;
  background: #fffbe6;
  border: 1px dashed #e6a23c;
  border-radius: 8px;

  .extra-label {
    display: block;
    font-size: 14px;
    font-weight: 500;
    color: #e6a23c;
    margin-bottom: 12px;
  }
}

// 上传区域
.upload-area {
  width: 100%;

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 30px;
    border-radius: 8px;
  }

  .upload-icon {
    font-size: 48px;
    color: #c0c4cc;
  }

  .upload-text {
    margin-top: 8px;
    color: #606266;
  }
}

// 错误提示
.field-error-text {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
}

// 底部操作
.form-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  .submit-btn {
    min-width: 140px;
  }
}

// 空状态
.not-found {
  text-align: center;
  padding: 60px 20px;
  color: #999;

  p {
    margin: 16px 0 24px;
    font-size: 16px;
  }
}

@media (max-width: 640px) {
  .form-container {
    padding: 16px;
  }

  .form-title-section {
    padding: 16px;

    h1 {
      font-size: 18px;
    }
  }

  .field-block {
    padding: 16px;
  }

  .option-item {
    padding: 12px;
  }

  .form-footer {
    flex-direction: column;

    .el-button {
      width: 100%;
    }
  }
}
</style>
