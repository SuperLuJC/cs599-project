<template>
  <div class="survey-preview">
    <div class="preview-header">
      <h2>{{ survey.title || '问卷预览' }}</h2>
      <p v-if="survey.description">{{ survey.description }}</p>
    </div>

    <el-form label-position="top" class="preview-form">
      <div
        v-for="(field, index) in survey.fields"
        :key="field.id"
        class="preview-field"
      >
        <!-- 题目标题行 -->
        <div class="field-header">
          <span class="field-index">{{ index + 1 }}.</span>
          <span class="field-title">{{ field.label }}</span>
          <span v-if="field.required" class="required-mark">*</span>
          <el-tag v-if="field.autoCalculate" type="success" size="small">自动计算</el-tag>
        </div>

        <!-- 题目内容区域 -->
        <div class="field-content">
          <!-- 单选题 - 每行一个选项 -->
          <div v-if="field.type === 'radio'" class="options-vertical">
            <el-radio-group v-model="formData[field.name]">
              <el-radio
                v-for="opt in field.options"
                :key="opt.value"
                :value="opt.value"
                size="large"
              >
                {{ opt.label }}
                <span v-if="opt.score > 0" class="score-tag">(+{{ opt.score }}分)</span>
              </el-radio>
            </el-radio-group>
            <div v-if="field.hasOther" class="other-option">
              <el-radio v-model="formData[field.name]" value="other" size="large">{{ field.otherLabel || '其他' }}</el-radio>
              <el-input placeholder="请输入其他内容" class="other-input" />
            </div>
          </div>

          <!-- 多选题 - 每行一个选项 -->
          <div v-else-if="field.type === 'checkbox'" class="options-vertical">
            <el-checkbox-group v-model="formData[field.name]">
              <el-checkbox
                v-for="opt in field.options"
                :key="opt.value"
                :value="opt.value"
                size="large"
              >
                {{ opt.label }}
                <span v-if="opt.score > 0" class="score-tag">(+{{ opt.score }}分)</span>
              </el-checkbox>
            </el-checkbox-group>
            <div v-if="field.hasOther" class="other-option">
              <el-checkbox v-model="formData[field.name]" value="other" size="large">{{ field.otherLabel || '其他' }}</el-checkbox>
              <el-input placeholder="请输入其他内容" class="other-input" />
            </div>
            <div v-if="field.minSelect > 0 || field.maxSelect > 0" class="select-hint">
              <el-tag size="small" type="info">
                {{ field.minSelect > 0 ? `至少选择${field.minSelect}项` : '' }}
                {{ field.maxSelect > 0 ? `最多选择${field.maxSelect}项` : '' }}
              </el-tag>
            </div>
          </div>

          <!-- 填空题 -->
          <el-input
            v-else-if="field.type === 'text'"
            v-model="formData[field.name]"
            :placeholder="field.placeholder || '请输入'"
            :maxlength="field.maxLength"
            show-word-limit
            size="large"
          />

          <!-- 多行文本 -->
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="formData[field.name]"
            type="textarea"
            :rows="field.rows || 4"
            :placeholder="field.placeholder || '请输入'"
            :maxlength="field.maxLength"
            show-word-limit
          />

          <!-- 下拉选择 -->
          <el-select
            v-else-if="field.type === 'select'"
            v-model="formData[field.name]"
            :placeholder="field.placeholder || '请选择'"
            size="large"
            style="width: 100%"
          >
            <el-option v-for="opt in field.options" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>

          <!-- 数字题 -->
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="formData[field.name]"
            :min="field.min"
            :max="field.max"
            :step="field.step || 1"
            :precision="field.precision || 0"
            size="large"
            style="width: 100%"
          />

          <!-- 日期题 -->
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="formData[field.name]"
            type="date"
            :placeholder="field.placeholder || '请选择日期'"
            :format="field.format || 'YYYY-MM-DD'"
            size="large"
            style="width: 100%"
          />

          <!-- 日期时间 -->
          <el-date-picker
            v-else-if="field.type === 'datetime'"
            v-model="formData[field.name]"
            type="datetime"
            :placeholder="field.placeholder || '请选择日期时间'"
            :format="field.format || 'YYYY-MM-DD HH:mm:ss'"
            size="large"
            style="width: 100%"
          />

          <!-- 评分题 -->
          <div v-else-if="field.type === 'rate'" class="rate-wrapper">
            <el-rate
              v-model="formData[field.name]"
              :max="field.max || 5"
              :allow-half="field.allowHalf"
              :show-text="field.showText"
              :texts="field.rateTexts ? field.rateTexts.split(',') : field.texts"
            />
          </div>

          <!-- 文件上传 -->
          <el-upload
            v-else-if="field.type === 'file'"
            action="/api/files/upload"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
          </el-upload>

          <!-- 自动计算题 -->
          <div v-else-if="field.autoCalculate" class="calculated-field">
            <span class="calculated-value">{{ formData[field.name] || 0 }}</span>
          </div>

          <!-- 默认 -->
          <el-input v-else v-model="formData[field.name]" :placeholder="field.placeholder || '请输入'" size="large" />
        </div>

        <!-- 帮助说明 -->
        <div v-if="field.helpText" class="help-text">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ field.helpText }}</span>
        </div>
      </div>
    </el-form>

    <div class="preview-footer" v-if="!readonly">
      <el-button type="primary" size="large">提交问卷</el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { UploadFilled, InfoFilled } from '@element-plus/icons-vue'

const props = defineProps({
  survey: {
    type: Object,
    required: true
  },
  readonly: {
    type: Boolean,
    default: true
  }
})

const formData = reactive({})

function handleUploadSuccess(fieldName, res) {
  if (res.code === 200) {
    formData[fieldName] = res.data.url
  }
}
</script>

<style scoped lang="scss">
.survey-preview {
  max-height: 70vh;
  overflow-y: auto;

  .preview-header {
    text-align: center;
    margin-bottom: 30px;
    padding-bottom: 20px;
    border-bottom: 1px solid #e4e7ed;

    h2 {
      margin: 0 0 10px;
      color: #303133;
      font-size: 22px;
    }

    p {
      color: #909399;
      margin: 0;
      line-height: 1.6;
    }
  }

  .preview-field {
    margin-bottom: 20px;
    padding: 20px;
    background: #f9fafc;
    border-radius: 12px;
    border: 1px solid #e4e7ed;
    display: flex;
    flex-direction: column;

    .field-header {
      margin-bottom: 15px;
      padding-bottom: 10px;
      border-bottom: 1px solid #ebeef5;
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
      justify-content: flex-start;
      text-align: left;

      .field-index {
        color: #409eff;
        font-weight: 600;
        font-size: 16px;
      }

      .field-title {
        font-weight: 600;
        color: #303133;
        font-size: 16px;
      }

      .required-mark {
        color: #f56c6c;
        font-size: 14px;
      }
    }

    .field-content {
      padding-left: 0;
      width: 100%;
      text-align: left;
    }

    // 选项垂直排列
    .options-vertical {
      width: 100%;
      text-align: left;

      :deep(.el-radio-group),
      :deep(.el-checkbox-group) {
        display: flex !important;
        flex-direction: column !important;
        gap: 10px;
        width: 100%;
        align-items: flex-start;
      }

      :deep(.el-radio),
      :deep(.el-checkbox) {
        display: flex !important;
        align-items: center;
        margin-right: 0 !important;
        width: 100% !important;
        padding: 12px 16px;
        background: #fff;
        border-radius: 8px;
        border: 1px solid #e4e7ed;
        transition: all 0.2s;
        box-sizing: border-box;
        justify-content: flex-start;
        text-align: left;

        &:hover {
          background: #ecf5ff;
          border-color: #409eff;
        }

        .score-tag {
          color: #67c23a;
          font-size: 12px;
          margin-left: 8px;
        }
      }

      :deep(.el-radio.is-checked),
      :deep(.el-checkbox.is-checked) {
        background: #ecf5ff;
        border-color: #409eff;
      }

      .other-option {
        display: flex;
        flex-direction: column;
        gap: 10px;
        margin-top: 10px;
        padding: 12px;
        background: #fff;
        border-radius: 8px;
        border: 1px dashed #e4e7ed;

        :deep(.el-radio),
        :deep(.el-checkbox) {
          border: none;
          padding: 0;
          background: transparent;
        }

        .other-input {
          width: 100%;
          margin-top: 8px;
        }
      }
    }

    .select-hint {
      margin-top: 10px;
    }

    .rate-wrapper {
      padding: 10px 0;
    }

    .calculated-field {
      padding: 16px;
      background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
      border-radius: 8px;
      text-align: center;

      .calculated-value {
        font-size: 24px;
        font-weight: 600;
        color: #67c23a;
      }
    }

    .help-text {
      margin-top: 12px;
      padding: 10px 12px;
      background: #f4f4f5;
      border-radius: 6px;
      font-size: 13px;
      color: #909399;
      display: flex;
      align-items: flex-start;
      gap: 6px;

      .el-icon {
        margin-top: 2px;
      }
    }
  }

  .preview-footer {
    text-align: center;
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid #e4e7ed;
  }
}
</style>