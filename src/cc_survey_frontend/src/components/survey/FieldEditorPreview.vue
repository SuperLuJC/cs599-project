<template>
  <div class="field-preview-wrapper">
    <!-- 题型标识 -->
    <div class="field-type-badge">
      <el-tag :type="getTypeTagType()" size="small">
        <el-icon><component :is="getTypeIcon()" /></el-icon>
        {{ getTypeLabel() }}
      </el-tag>
    </div>

    <!-- 单选题预览 -->
    <div v-if="field.type === 'radio'" class="preview-radio">
      <div class="options-list">
        <div v-for="opt in field.options" :key="opt.value" class="option-row">
          <el-radio disabled :value="opt.value" size="large">
            <span class="option-label">{{ opt.label }}</span>
            <span v-if="opt.score > 0" class="score-tag">(+{{ opt.score }}分)</span>
          </el-radio>
          <el-tag v-if="opt.checked" type="success" size="small" class="default-tag">默认选中</el-tag>
        </div>
      </div>
      <div v-if="field.hasOther" class="other-option">
        <el-radio disabled value="other" size="large">
          {{ field.otherLabel || '其他' }}
        </el-radio>
        <el-input disabled placeholder="请输入其他内容" class="other-input" />
      </div>
    </div>

    <!-- 多选题预览 -->
    <div v-else-if="field.type === 'checkbox'" class="preview-checkbox">
      <div class="options-list">
        <div v-for="opt in field.options" :key="opt.value" class="option-row">
          <el-checkbox disabled :value="opt.value" size="large">
            <span class="option-label">{{ opt.label }}</span>
            <span v-if="opt.score > 0" class="score-tag">(+{{ opt.score }}分)</span>
          </el-checkbox>
          <el-tag v-if="opt.checked" type="success" size="small" class="default-tag">默认选中</el-tag>
        </div>
      </div>
      <div v-if="field.hasOther" class="other-option">
        <el-checkbox disabled value="other" size="large">
          {{ field.otherLabel || '其他' }}
        </el-checkbox>
        <el-input disabled placeholder="请输入其他内容" class="other-input" />
      </div>
      <div v-if="field.minSelect > 0 || field.maxSelect > 0" class="select-limit">
        <el-tag size="small" type="info">
          {{ field.minSelect > 0 ? `至少选${field.minSelect}项` : '' }}
          {{ field.maxSelect > 0 ? `最多选${field.maxSelect}项` : '' }}
        </el-tag>
      </div>
    </div>

    <!-- 填空题预览 -->
    <div v-else-if="field.type === 'text'" class="preview-text">
      <el-input
        :placeholder="field.placeholder || '请输入'"
        :maxlength="field.maxLength"
        show-word-limit
        disabled
      />
      <div class="validation-info">
        <el-tag v-if="field.minLength > 0 || field.maxLength > 0" size="small" type="info">
          {{ field.minLength || 0 }} ~ {{ field.maxLength || 200 }} 字符
        </el-tag>
        <el-tag v-if="field.pattern" size="small" type="warning">
          正则验证
        </el-tag>
      </div>
    </div>

    <!-- 多行文本预览 -->
    <div v-else-if="field.type === 'textarea'" class="preview-textarea">
      <el-input
        type="textarea"
        :rows="field.rows || 4"
        :placeholder="field.placeholder || '请输入'"
        :maxlength="field.maxLength"
        show-word-limit
        disabled
      />
      <div class="length-info">
        <el-tag size="small" type="info">
          {{ field.minLength || 0 }} ~ {{ field.maxLength || 1000 }} 字符
        </el-tag>
      </div>
    </div>

    <!-- 下拉选择预览 -->
    <div v-else-if="field.type === 'select'" class="preview-select">
      <el-select :placeholder="field.placeholder || '请选择'" disabled style="width: 100%">
        <el-option
          v-for="opt in field.options"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <div v-if="field.hasOther" class="other-option">
        <el-input disabled placeholder="请输入其他内容" />
      </div>
    </div>

    <!-- 数字题预览 -->
    <div v-else-if="field.type === 'number'" class="preview-number">
      <el-input-number
        :min="field.min"
        :max="field.max"
        :step="field.step || 1"
        :precision="field.precision || 0"
        disabled
        style="width: 200px"
      />
      <div class="range-info">
        <el-tag size="small" type="info">
          范围: {{ field.min }} ~ {{ field.max }}
          <span v-if="field.precision > 0"> (保留{{ field.precision }}位小数)</span>
        </el-tag>
      </div>
    </div>

    <!-- 日期题预览 -->
    <div v-else-if="field.type === 'date'" class="preview-date">
      <el-date-picker
        type="date"
        :placeholder="field.placeholder || '请选择日期'"
        :format="field.format || 'YYYY-MM-DD'"
        disabled
        style="width: 100%"
      />
      <div v-if="field.minDate || field.maxDate" class="date-range">
        <el-tag size="small" type="info">
          {{ field.minDate ? `从 ${field.minDate}` : '' }}
          {{ field.maxDate ? `至 ${field.maxDate}` : '' }}
        </el-tag>
      </div>
    </div>

    <!-- 日期时间预览 -->
    <div v-else-if="field.type === 'datetime'" class="preview-datetime">
      <el-date-picker
        type="datetime"
        :placeholder="field.placeholder || '请选择日期时间'"
        :format="field.format || 'YYYY-MM-DD HH:mm:ss'"
        disabled
        style="width: 100%"
      />
    </div>

    <!-- 评分题预览 -->
    <div v-else-if="field.type === 'rate'" class="preview-rate">
      <el-rate
        :max="field.max || 5"
        :allow-half="field.allowHalf"
        :show-text="field.showText"
        :texts="field.rateTexts ? field.rateTexts.split(',') : field.texts"
        disabled
      />
      <div class="rate-info">
        <el-tag size="small" type="info">
          {{ field.max || 5 }} 星评分
          <span v-if="field.allowHalf">(可半星)</span>
        </el-tag>
      </div>
    </div>

    <!-- 文件上传预览 -->
    <div v-else-if="field.type === 'file'" class="preview-file">
      <el-upload disabled drag>
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
      </el-upload>
      <div class="file-info">
        <el-tag size="small" type="info">
          最多 {{ field.maxFiles || 1 }} 个文件
        </el-tag>
        <el-tag size="small" type="info">
          最大 {{ field.maxSize || 10 }}MB
        </el-tag>
        <el-tag size="small" type="warning" v-if="field.acceptTypes && field.acceptTypes.length">
          {{ getFileTypesText(field.acceptTypes) }}
        </el-tag>
      </div>
    </div>

    <!-- 默认 -->
    <div v-else class="preview-default">
      <el-input :placeholder="field.placeholder || '请输入'" disabled />
    </div>

    <!-- 条件显示提示 -->
    <div v-if="field.hasCondition" class="condition-info">
      <el-tag size="small" type="warning">
        <el-icon><Warning /></el-icon>
        条件显示: {{ getConditionText() }}
      </el-tag>
    </div>

    <!-- 自动计算提示 -->
    <div v-if="field.autoCalculate" class="calculate-info">
      <el-tag size="small" type="success">
        <el-icon><DataAnalysis /></el-icon>
        自动计算: {{ field.calculateFormula || '未设置公式' }}
      </el-tag>
    </div>

    <!-- 帮助说明 -->
    <div v-if="field.helpText" class="help-text">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ field.helpText }}</span>
    </div>
  </div>
</template>

<script setup>
import { UploadFilled, Warning, InfoFilled, Calendar, Select, Checked, Edit, Document, Grid, Star, DataAnalysis } from '@element-plus/icons-vue'

const props = defineProps({
  field: {
    type: Object,
    required: true
  }
})

function getTypeLabel() {
  const typeMap = {
    radio: '单选题',
    checkbox: '多选题',
    text: '填空题',
    textarea: '多行文本',
    select: '下拉选择',
    number: '数字题',
    date: '日期题',
    datetime: '日期时间',
    rate: '评分题',
    file: '文件上传'
  }
  return typeMap[props.field.type] || '未知类型'
}

function getTypeIcon() {
  const iconMap = {
    radio: Select,
    checkbox: Checked,
    text: Edit,
    textarea: Document,
    select: Grid,
    number: Edit,
    date: Calendar,
    datetime: Calendar,
    rate: Star,
    file: UploadFilled
  }
  return iconMap[props.field.type] || Edit
}

function getTypeTagType() {
  const typeMap = {
    radio: '',
    checkbox: 'success',
    text: 'info',
    textarea: 'info',
    select: 'warning',
    number: 'info',
    date: 'warning',
    datetime: 'warning',
    rate: 'danger',
    file: ''
  }
  return typeMap[props.field.type] || ''
}

function getFileTypesText(types) {
  if (!types || types.length === 0) return ''
  if (types.includes('*/*')) return '所有类型'
  const typeMap = {
    'image/*': '图片',
    '.pdf': 'PDF',
    '.doc,.docx': 'Word',
    '.xls,.xlsx': 'Excel',
    '.zip,.rar': '压缩包'
  }
  return types.map(t => typeMap[t] || t).join(', ')
}

function getConditionText() {
  const f = props.field
  const opMap = {
    eq: '等于',
    neq: '不等于',
    contains: '包含',
    notEmpty: '不为空',
    empty: '为空',
    gt: '大于',
    gte: '大于等于',
    lt: '小于',
    lte: '小于等于'
  }
  const conditionValue = Array.isArray(f.conditionValue) ? f.conditionValue.join(',') : f.conditionValue
  return `${f.conditionField} ${opMap[f.conditionOperator] || f.conditionOperator} ${conditionValue || '任意值'}`
}
</script>

<style scoped lang="scss">
.field-preview-wrapper {
  .field-type-badge {
    margin-bottom: 12px;

    .el-tag {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  .options-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .option-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 8px;
    border: 1px solid #e4e7ed;
    transition: all 0.2s;

    &:hover {
      background: #ecf5ff;
      border-color: #409eff;
    }

    .el-radio,
    .el-checkbox {
      margin-right: 0;

      .option-label {
        font-size: 14px;
        color: #303133;
      }
    }
  }

  .score-tag {
    color: #67c23a;
    font-size: 12px;
    margin-left: 8px;
    font-weight: 500;
  }

  .default-tag {
    flex-shrink: 0;
  }

  .other-option {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 10px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 8px;
    border: 1px dashed #e4e7ed;

    .el-radio,
    .el-checkbox {
      margin-right: 0;
    }

    .other-input {
      width: 100%;
    }
  }

  .select-limit, .length-info, .range-info, .date-range, .rate-info, .file-info, .validation-info {
    margin-top: 12px;
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .condition-info, .calculate-info {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed #e4e7ed;

    .el-tag {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  .help-text {
    margin-top: 12px;
    padding: 12px;
    background: #f4f4f5;
    border-radius: 8px;
    font-size: 13px;
    color: #909399;
    display: flex;
    align-items: flex-start;
    gap: 6px;

    .el-icon {
      margin-top: 2px;
    }
  }

  .preview-file {
    :deep(.el-upload-dragger) {
      width: 100%;
    }
  }

  .preview-text, .preview-textarea {
    .validation-info {
      gap: 8px;
    }
  }
}
</style>