<template>
  <div class="field-previews">
    <!-- 单选题预览 -->
    <div v-if="field.type === 'radio'" class="preview-radio">
      <el-radio-group disabled>
        <el-radio v-for="opt in field.options" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </el-radio>
      </el-radio-group>
    </div>

    <!-- 多选题预览 -->
    <div v-else-if="field.type === 'checkbox'" class="preview-checkbox">
      <el-checkbox-group disabled>
        <el-checkbox v-for="opt in field.options" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </el-checkbox>
      </el-checkbox-group>
    </div>

    <!-- 填空题预览 -->
    <div v-else-if="field.type === 'text'" class="preview-text">
      <el-input :placeholder="field.placeholder || '请输入'" disabled />
    </div>

    <!-- 多行文本预览 -->
    <div v-else-if="field.type === 'textarea'" class="preview-textarea">
      <el-input type="textarea" :rows="3" :placeholder="field.placeholder || '请输入'" disabled />
    </div>

    <!-- 下拉选择预览 -->
    <div v-else-if="field.type === 'select'" class="preview-select">
      <el-select :placeholder="field.placeholder || '请选择'" disabled>
        <el-option v-for="opt in field.options" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
    </div>

    <!-- 数字题预览 -->
    <div v-else-if="field.type === 'number'" class="preview-number">
      <el-input-number :min="field.min" :max="field.max" disabled />
    </div>

    <!-- 日期题预览 -->
    <div v-else-if="field.type === 'date'" class="preview-date">
      <el-date-picker :placeholder="field.placeholder || '请选择日期'" disabled />
    </div>

    <!-- 评分题预览 -->
    <div v-else-if="field.type === 'rate'" class="preview-rate">
      <el-rate :max="field.max || 5" disabled />
    </div>

    <!-- 文件上传预览 -->
    <div v-else-if="field.type === 'file'" class="preview-file">
      <el-upload disabled>
        <el-button type="primary" disabled>点击上传</el-button>
      </el-upload>
    </div>

    <!-- 默认 -->
    <div v-else class="preview-default">
      <el-input :placeholder="field.placeholder || '请输入'" disabled />
    </div>
  </div>
</template>

<script setup>
defineProps({
  field: {
    type: Object,
    required: true
  },
  preview: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped lang="scss">
.field-previews {
  .el-radio-group, .el-checkbox-group {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .el-select {
    width: 100%;
  }

  .el-input-number {
    width: 200px;
  }
}
</style>