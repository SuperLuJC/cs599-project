<template>
  <div class="survey-editor">
    <!-- 左侧题型面板 -->
    <div class="field-panel">
      <h3>题型组件</h3>
      <div class="field-types">
        <div
          v-for="type in fieldTypes"
          :key="type.type"
          class="field-type-card"
          draggable="true"
          @dragstart="onDragStart($event, type)"
        >
          <div class="field-icon">
            <el-icon :size="24"><component :is="type.icon" /></el-icon>
          </div>
          <span>{{ type.label }}</span>
        </div>
      </div>
    </div>

    <!-- 中间编辑区域 -->
    <div class="editor-main">
      <div class="editor-header">
        <el-input
          v-model="surveyTitle"
          placeholder="请输入问卷标题"
          class="title-input"
        />
        <el-input
          v-model="surveyDesc"
          type="textarea"
          :rows="2"
          placeholder="请输入问卷描述（可选）"
          class="desc-input"
        />
      </div>

      <div
        class="fields-container"
        @dragover.prevent
        @drop="onDrop"
      >
        <div v-if="fields.length === 0" class="empty-placeholder">
          <el-icon :size="48"><DocumentAdd /></el-icon>
          <p>从左侧拖拽题型到此处</p>
        </div>

        <div
          v-for="(field, index) in fields"
          :key="field.id"
          class="field-item"
          :class="{ active: activeFieldId === field.id }"
          @click="selectField(field)"
        >
          <!-- 题号和操作按钮 -->
          <div class="field-header">
            <div class="field-info">
              <div class="field-title-row">
                <span class="index-num">{{ index + 1 }}.</span>
                <span class="field-title">{{ field.label }}</span>
              </div>
              <div class="field-tags" v-if="field.required || field.hasCondition || field.autoCalculate">
                <el-tag v-if="field.required" type="danger" size="small" effect="plain">必填</el-tag>
                <el-tag v-if="field.hasCondition" type="warning" size="small" effect="plain">条件显示</el-tag>
                <el-tag v-if="field.autoCalculate" type="success" size="small" effect="plain">自动计算</el-tag>
              </div>
            </div>
            <div class="field-actions">
              <el-button link @click.stop="moveField(index, -1)" :disabled="index === 0">
                <el-icon><Top /></el-icon>
              </el-button>
              <el-button link @click.stop="moveField(index, 1)" :disabled="index === fields.length - 1">
                <el-icon><Bottom /></el-icon>
              </el-button>
              <el-button link type="danger" @click.stop="removeField(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- 题目详细预览 -->
          <div class="field-preview">
            <FieldEditorPreview :field="field" />
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧属性面板 -->
    <div class="property-panel" v-if="activeField">
      <h3>题目属性</h3>
      <el-form label-position="top" class="property-form">
        <el-collapse v-model="activeCollapse">
          <el-collapse-item title="基础设置" name="basic">
            <el-form-item label="题目标题">
              <el-input v-model="activeField.label" placeholder="请输入题目标题" />
            </el-form-item>

            <el-form-item label="字段ID">
              <el-input v-model="activeField.id" placeholder="如: q1, q2" />
              <span class="form-tip">用于数据引用和计算公式</span>
            </el-form-item>

            <el-form-item label="字段名称">
              <el-input v-model="activeField.name" placeholder="字段标识（英文）" />
            </el-form-item>

            <el-form-item label="是否必填">
              <el-switch v-model="activeField.required" :disabled="activeField.autoCalculate || activeField.readonly" />
              <span v-if="activeField.autoCalculate || activeField.readonly" class="form-tip">自动计算/只读题无需填写</span>
            </el-form-item>

            <el-form-item v-if="['text', 'textarea', 'number', 'select', 'input'].includes(activeField.type)" label="占位提示">
              <el-input v-model="activeField.placeholder" placeholder="输入框内的提示文字" />
              <span class="form-tip">当用户未输入时显示的灰色提示文字</span>
            </el-form-item>

            <el-form-item label="题目提示">
              <el-input v-model="activeField.hint" type="textarea" :rows="2" placeholder="显示在题目下方的提示文字（支持HTML）" />
              <span class="form-tip">可使用 &lt;a&gt; 标签添加链接</span>
            </el-form-item>

            <el-form-item v-if="['radio', 'checkbox', 'text', 'input'].includes(activeField.type)" label="是否只读">
              <el-switch v-model="activeField.readonly" />
              <span class="form-tip">只读字段用于显示计算结果</span>
            </el-form-item>

            <el-form-item label="帮助说明">
              <el-input v-model="activeField.helpText" type="textarea" :rows="2" placeholder="显示在题目下方的说明文字" />
            </el-form-item>
          </el-collapse-item>

          <!-- 选项配置 -->
          <el-collapse-item v-if="hasOptions" title="选项设置" name="options">
            <div class="options-editor">
              <div
                v-for="(opt, optIndex) in activeField.options"
                :key="optIndex"
                class="option-item"
              >
                <div class="option-row">
                  <el-checkbox v-model="opt.checked" title="设为默认选中" class="default-check" />
                  <el-input v-model="opt.label" placeholder="选项文字" class="option-label" />
                  <el-input-number v-model="opt.score" :min="0" :step="0.5" size="small" class="option-score" placeholder="分数" />
                  <el-button link type="danger" @click="removeOption(optIndex)">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
                <div class="option-value">
                  <span>选项值:</span>
                  <el-input v-model="opt.value" size="small" placeholder="A" />
                </div>
                <div class="option-hint-row">
                  <span>选项提示:</span>
                  <el-input v-model="opt.hint" size="small" placeholder="选中此选项时显示的提示信息" />
                </div>
              </div>
              <div class="option-actions">
                <el-button type="primary" link @click="addOption">
                  <el-icon><Plus /></el-icon> 添加选项
                </el-button>
                <el-checkbox v-model="activeField.hasOther" @change="toggleOtherOption">添加"其他"选项</el-checkbox>
              </div>
              <!-- 多选题限制 -->
              <template v-if="activeField.type === 'checkbox'">
                <div class="select-limit">
                  <span>选择数量限制：</span>
                  <el-input-number v-model="activeField.minSelect" :min="0" size="small" placeholder="最少" />
                  <span>~</span>
                  <el-input-number v-model="activeField.maxSelect" :min="0" size="small" placeholder="最多" />
                  <span>项</span>
                </div>
              </template>
              <!-- 额外输入配置 -->
              <div class="extra-input-config">
                <el-checkbox v-model="hasExtraInput" @change="toggleExtraInput">选项触发额外输入</el-checkbox>
                <template v-if="activeField.extraInput">
                  <div class="extra-input-fields">
                    <el-form-item label="额外输入类型">
                      <el-select v-model="activeField.extraInput.type" style="width: 100%">
                        <el-option label="文本输入" value="input" />
                        <el-option label="数字输入" value="number" />
                        <el-option label="文件上传" value="upload" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="提示文字">
                      <el-input v-model="activeField.extraInput.hint" placeholder="请输入提示文字" />
                    </el-form-item>
                    <el-form-item label="占位符">
                      <el-input v-model="activeField.extraInput.placeholder" placeholder="请输入占位符" />
                    </el-form-item>
                    <el-form-item label="是否必填">
                      <el-switch v-model="activeField.extraInput.required" />
                    </el-form-item>
                    <el-form-item v-if="activeField.extraInput.type === 'upload'" label="按钮文字">
                      <el-input v-model="activeField.extraInput.btnText" placeholder="点击上传" />
                    </el-form-item>
                    <el-form-item label="显示条件">
                      <el-input v-model="activeField.extraInput.showWhen" placeholder="如: q1 && q1 !== 'A'" />
                      <span class="form-tip">留空则选中非"无"选项时显示</span>
                    </el-form-item>
                  </div>
                </template>
              </div>
            </div>
          </el-collapse-item>

          <!-- 组合输入配置 -->
          <el-collapse-item v-if="activeField.type === 'multi-input'" title="子字段设置" name="subfields">
            <div class="subfields-editor">
              <div
                v-for="(subField, subIndex) in activeField.subFields"
                :key="subIndex"
                class="subfield-item"
              >
                <div class="subfield-header">
                  <span>子字段 {{ subIndex + 1 }}</span>
                  <el-button link type="danger" @click="removeSubField(subIndex)">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
                <div class="subfield-body">
                  <el-form-item label="字段ID">
                    <el-input v-model="subField.id" placeholder="如: q1_1" />
                  </el-form-item>
                  <el-form-item label="标签">
                    <el-input v-model="subField.label" placeholder="字段标签" />
                  </el-form-item>
                  <el-form-item label="输入类型">
                    <el-select v-model="subField.inputType" style="width: 100%">
                      <el-option label="文本" value="text" />
                      <el-option label="数字" value="number" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="占位符">
                    <el-input v-model="subField.placeholder" placeholder="输入提示" />
                  </el-form-item>
                  <el-form-item label="是否必填">
                    <el-switch v-model="subField.required" />
                  </el-form-item>
                  <el-form-item label="是否只读">
                    <el-switch v-model="subField.readonly" />
                  </el-form-item>
                  <el-form-item label="提示信息">
                    <el-input v-model="subField.hint" type="textarea" :rows="2" placeholder="字段提示" />
                  </el-form-item>
                  <el-form-item label="计算公式">
                    <el-input v-model="subField.computeRule" placeholder="如: (q1_1 && q1_2) ? (q1_2 / q1_1 * 100).toFixed(2) : ''" />
                    <span class="form-tip">根据其他字段自动计算</span>
                  </el-form-item>
                                    <el-form-item label="验证规则">
                    <el-input :model-value="getSubFieldValidatorLogic(subIndex)" @update:model-value="setSubFieldValidatorLogic(subIndex, $event)" placeholder="如: Number(q1_2) <= Number(q1_1)" />
                  </el-form-item>
                  <el-form-item label="验证失败提示">
                    <el-input :model-value="getSubFieldValidatorMessage(subIndex)" @update:model-value="setSubFieldValidatorMessage(subIndex, $event)" placeholder="验证失败时显示的消息" />
                  </el-form-item>
                </div>
              </div>
              <el-button type="primary" link @click="addSubField">
                <el-icon><Plus /></el-icon> 添加子字段
              </el-button>
            </div>
          </el-collapse-item>

          <!-- 验证规则 -->
          <el-collapse-item title="验证规则" name="validation">
            <!-- 文本验证 -->
            <template v-if="['text', 'textarea'].includes(activeField.type)">
              <div class="validation-section">
                <div class="validation-row">
                  <span>字符长度限制：</span>
                  <el-input-number v-model="activeField.minLength" :min="0" :max="10000" size="small" />
                  <span>~</span>
                  <el-input-number v-model="activeField.maxLength" :min="1" :max="10000" size="small" />
                  <span>字符</span>
                </div>
                <div class="validation-row">
                  <span>正则验证：</span>
                  <el-input v-model="activeField.pattern" placeholder="如: ^1[3-9]\d{9}$ 验证手机号" />
                </div>
                <div class="validation-row">
                  <span>验证失败提示：</span>
                  <el-input v-model="activeField.patternMessage" placeholder="如: 请输入正确的手机号" />
                </div>
                <div class="validation-presets">
                  <span>常用正则：</span>
                  <el-tag @click="applyPattern('phone')" size="small">手机号</el-tag>
                  <el-tag @click="applyPattern('email')" size="small">邮箱</el-tag>
                  <el-tag @click="applyPattern('idcard')" size="small">身份证</el-tag>
                  <el-tag @click="applyPattern('number')" size="small">纯数字</el-tag>
                  <el-tag @click="applyPattern('chinese')" size="small">中文</el-tag>
                </div>
              </div>
            </template>

            <!-- 数字验证 -->
            <template v-if="activeField.type === 'number'">
              <div class="validation-section">
                <div class="validation-row">
                  <span>数值范围：</span>
                  <el-input-number v-model="activeField.min" size="small" />
                  <span>~</span>
                  <el-input-number v-model="activeField.max" size="small" />
                </div>
                <div class="validation-row">
                  <span>步长：</span>
                  <el-input-number v-model="activeField.step" :min="0.01" :step="0.1" size="small" />
                </div>
                <div class="validation-row">
                  <span>小数位数：</span>
                  <el-input-number v-model="activeField.precision" :min="0" :max="10" size="small" />
                </div>
              </div>
            </template>

            <!-- 文件验证 -->
            <template v-if="activeField.type === 'file'">
              <div class="validation-section">
                <div class="validation-row">
                  <span>最大文件数：</span>
                  <el-input-number v-model="activeField.maxFiles" :min="1" :max="20" size="small" />
                </div>
                <div class="validation-row">
                  <span>单文件大小上限：</span>
                  <el-input-number v-model="activeField.maxSize" :min="1" :max="100" size="small" />
                  <span>MB</span>
                </div>
                <div class="validation-row">
                  <span>允许的文件类型：</span>
                </div>
                <el-select v-model="activeField.acceptTypes" multiple placeholder="选择文件类型" style="width: 100%">
                  <el-option label="图片 (jpg, png, gif, webp)" value="image/*" />
                  <el-option label="PDF文档" value=".pdf" />
                  <el-option label="Word文档" value=".doc,.docx" />
                  <el-option label="Excel表格" value=".xls,.xlsx" />
                  <el-option label="压缩文件" value=".zip,.rar" />
                  <el-option label="所有文件" value="*/*" />
                </el-select>
              </div>
            </template>

            <!-- 日期验证 -->
            <template v-if="['date', 'datetime'].includes(activeField.type)">
              <div class="validation-section">
                <div class="validation-row">
                  <span>日期范围：</span>
                </div>
                <el-date-picker v-model="activeField.minDate" type="date" placeholder="最早日期" style="width: 48%" />
                <el-date-picker v-model="activeField.maxDate" type="date" placeholder="最晚日期" style="width: 48%; margin-left: 4%" />
              </div>
            </template>

            <!-- 评分验证 -->
            <template v-if="activeField.type === 'rate'">
              <div class="validation-section">
                <div class="validation-row">
                  <span>最大分值：</span>
                  <el-input-number v-model="activeField.max" :min="1" :max="10" size="small" />
                </div>
                <div class="validation-row">
                  <el-checkbox v-model="activeField.allowHalf">允许半星</el-checkbox>
                </div>
                <div class="validation-row">
                  <el-checkbox v-model="activeField.showText">显示评分文字</el-checkbox>
                </div>
                <template v-if="activeField.showText">
                  <div class="validation-row">
                    <span>评分文字（逗号分隔）：</span>
                  </div>
                  <el-input v-model="activeField.rateTexts" placeholder="极差,较差,一般,较好,极好" />
                </template>
              </div>
            </template>
          </el-collapse-item>

          <!-- 自动计算 -->
          <el-collapse-item title="自动计算" name="calculate">
            <div class="calculate-editor">
              <el-checkbox v-model="activeField.autoCalculate">启用自动计算</el-checkbox>
              <template v-if="activeField.autoCalculate">
                <div class="calculate-desc">
                  <el-icon><InfoFilled /></el-icon>
                  <span>此题将根据其他题目的答案自动计算，用户无需填写</span>
                </div>
                <div class="calculate-rule">
                  <div class="calculate-row">
                    <span>计算公式：</span>
                  </div>
                  <el-input
                    v-model="activeField.calculateFormula"
                    type="textarea"
                    :rows="3"
                    placeholder="如: field_1 + field_2 或 field_1 * 0.5 + field_3"
                  />
                  <div class="formula-help">
                    <p>可用变量：前面题目的字段ID（如 q1, q2_1）</p>
                    <p>支持运算：+ - * / ( ) 及函数 sum, avg, max, min</p>
                    <p>示例：sum(q1, q2) 或 q1 * 100 / q2</p>
                  </div>
                  <div class="calculate-row">
                    <span>结果格式：</span>
                    <el-select v-model="activeField.calculateFormat" style="width: 100%">
                      <el-option label="数字（保留整数）" value="integer" />
                      <el-option label="数字（保留1位小数）" value="decimal1" />
                      <el-option label="数字（保留2位小数）" value="decimal2" />
                      <el-option label="百分比" value="percent" />
                      <el-option label="文本" value="text" />
                    </el-select>
                  </div>
                  <div class="calculate-row" v-if="activeField.calculateFormat === 'text'">
                    <span>文本模板：</span>
                    <el-input v-model="activeField.calculateTemplate" placeholder="如: 总分：{result} 分" />
                  </div>
                </div>
              </template>
            </div>
            <!-- 计算规则（另一种方式） -->
            <div class="compute-rule-editor" v-if="!activeField.autoCalculate">
              <el-form-item label="计算规则">
                <el-input
                  v-model="activeField.computeRule"
                  type="textarea"
                  :rows="2"
                  placeholder="如: q3_3 !== '' ? (q3_3 <= 10 ? 'A' : 'B') : ''"
                />
                <span class="form-tip">根据其他字段值自动设置此字段值</span>
              </el-form-item>
            </div>
          </el-collapse-item>

          <!-- 条件显示 -->
          <el-collapse-item title="条件显示" name="condition">
            <div class="condition-editor">
              <el-checkbox v-model="activeField.hasCondition">启用条件显示</el-checkbox>
              <template v-if="activeField.hasCondition">
                <div class="condition-desc">
                  <el-icon><InfoFilled /></el-icon>
                  <span>当满足条件时，此题才会显示给用户</span>
                </div>
                <div class="condition-rule">
                  <div class="condition-row">
                    <span>显示条件表达式：</span>
                    <el-input
                      v-model="activeField.showWhen"
                      type="textarea"
                      :rows="2"
                      placeholder="如: q1 && q1 !== 'A' 或 q2 && q2.includes('B')"
                    />
                    <span class="form-tip">使用字段ID引用其他题目的值</span>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="condition-rule">
                  <div class="condition-row">
                    <span>依赖题目：</span>
                    <el-select v-model="activeField.conditionField" placeholder="选择触发条件的题目" style="width: 100%">
                      <el-option
                        v-for="f in getConditionFields()"
                        :key="f.id"
                        :label="`${f.label} (${f.id})`"
                        :value="f.id"
                      />
                    </el-select>
                  </div>
                  <div class="condition-row" v-if="activeField.conditionField">
                    <span>判断条件：</span>
                    <el-select v-model="activeField.conditionOperator" placeholder="选择条件" style="width: 100%">
                      <el-option-group label="选项类条件">
                        <el-option label="选中了" value="eq" />
                        <el-option label="未选中" value="neq" />
                        <el-option label="包含选项" value="contains" />
                      </el-option-group>
                      <el-option-group label="数值类条件" v-if="getConditionFieldType() === 'number'">
                        <el-option label="大于" value="gt" />
                        <el-option label="大于等于" value="gte" />
                        <el-option label="小于" value="lt" />
                        <el-option label="小于等于" value="lte" />
                        <el-option label="等于" value="eq" />
                      </el-option-group>
                      <el-option-group label="通用条件">
                        <el-option label="不为空" value="notEmpty" />
                        <el-option label="为空" value="empty" />
                      </el-option-group>
                    </el-select>
                  </div>
                  <div class="condition-row" v-if="activeField.conditionField && needsConditionValue()">
                    <span>比较值：</span>
                    <template v-if="getConditionFieldType() === 'radio' || getConditionFieldType() === 'select'">
                      <el-select v-model="activeField.conditionValue" placeholder="选择选项" style="width: 100%">
                        <el-option
                          v-for="opt in getConditionFieldOptions()"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                        />
                      </el-select>
                    </template>
                    <template v-else-if="getConditionFieldType() === 'checkbox'">
                      <el-select v-model="activeField.conditionValue" multiple placeholder="选择选项" style="width: 100%">
                        <el-option
                          v-for="opt in getConditionFieldOptions()"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                        />
                      </el-select>
                    </template>
                    <template v-else>
                      <el-input v-model="activeField.conditionValue" placeholder="输入比较值" />
                    </template>
                  </div>
                </div>
              </template>
            </div>
          </el-collapse-item>

          <!-- 高级设置 -->
          <el-collapse-item title="高级设置" name="advanced">
            <el-form-item label="默认值">
              <el-input v-model="activeField.defaultValue" placeholder="默认值" />
            </el-form-item>
            <el-form-item label="字段宽度">
              <el-radio-group v-model="activeField.width">
                <el-radio label="full">整行</el-radio>
                <el-radio label="half">半行</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
    </div>

    <!-- 右侧问卷设置面板（未选中题目时显示） -->
    <div class="property-panel survey-settings-panel" v-else>
      <h3>问卷设置</h3>
      <el-form label-position="top" class="property-form">
        <el-collapse v-model="surveySettingsCollapse">
          <!-- 基础设置 -->
          <el-collapse-item title="基础设置" name="basic">
            <el-form-item label="问卷标题">
              <el-input v-model="surveyTitle" placeholder="请输入问卷标题" />
            </el-form-item>
            <el-form-item label="问卷描述">
              <el-input v-model="surveyDesc" type="textarea" :rows="3" placeholder="请输入问卷描述（可选）" />
            </el-form-item>
          </el-collapse-item>

          <!-- 提交设置 -->
          <el-collapse-item title="提交设置" name="submission">
            <el-form-item label="最大提交份数">
              <div class="submission-limit-setting">
                <el-checkbox v-model="enableSubmissionLimit">限制提交份数</el-checkbox>
                <template v-if="enableSubmissionLimit">
                  <el-input-number
                    v-model="maxSubmissions"
                    :min="1"
                    :max="999999"
                    :step="100"
                    style="width: 100%"
                  />
                  <span class="form-tip">达到此数量后将无法继续提交</span>
                </template>
              </div>
            </el-form-item>

            <el-form-item label="允许修改提交">
              <el-switch v-model="allowEdit" />
              <span class="form-tip">开启后用户可以修改已提交的答案</span>
            </el-form-item>

            <el-form-item label="允许匿名提交">
              <el-switch v-model="allowAnonymous" />
              <span class="form-tip">开启后无需登录即可填写</span>
            </el-form-item>
          </el-collapse-item>

          <!-- 有效期设置 -->
          <el-collapse-item title="有效期设置" name="period">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="startTime"
                type="datetime"
                placeholder="不限制开始时间"
                style="width: 100%"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
              <span class="form-tip">问卷在此时间后才能填写</span>
            </el-form-item>
            <el-form-item label="结束时间">
              <el-date-picker
                v-model="endTime"
                type="datetime"
                placeholder="不限制结束时间"
                style="width: 100%"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
              <span class="form-tip">问卷在此时间后将无法填写</span>
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
    </div>

    <!-- 底部操作栏 -->
    <div class="editor-footer">
      <el-button @click="handlePreview">
        <el-icon><View /></el-icon> 预览
      </el-button>
      <el-button @click="handleSaveDraft">
        <el-icon><Document /></el-icon> 保存草稿
      </el-button>
      <el-button type="primary" @click="handlePublish">
        <el-icon><Promotion /></el-icon> 发布问卷
      </el-button>
    </div>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewVisible" title="问卷预览" width="900px" destroy-on-close top="5vh">
      <SurveyPreview :survey="previewData" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DocumentAdd, Top, Bottom, Delete, Close, Plus, View, Document, Promotion,
  Edit, Select, Checked, Calendar, Star, Upload, Grid, InfoFilled
} from '@element-plus/icons-vue'
import SurveyPreview from './SurveyPreview.vue'
import FieldEditorPreview from './FieldEditorPreview.vue'

const props = defineProps({
  initialData: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['save', 'publish'])

// 问卷基本信息
const surveyTitle = ref('')
const surveyDesc = ref('')

// 问卷设置
const surveySettingsCollapse = ref(['basic', 'submission', 'period'])
const enableSubmissionLimit = ref(false)
const maxSubmissions = ref(100)
const allowEdit = ref(false)
const allowAnonymous = ref(true)
const startTime = ref(null)
const endTime = ref(null)

// 字段列表
const fields = ref([])
const activeFieldId = ref(null)
const activeField = computed(() => fields.value.find(f => f.id === activeFieldId.value))
const activeCollapse = ref(['basic'])

// 预览
const previewVisible = ref(false)
const previewData = ref({})

// 判断是否有选项
const hasOptions = computed(() => {
  return activeField.value && ['radio', 'checkbox', 'select'].includes(activeField.value.type)
})

// 判断是否有额外输入
const hasExtraInput = computed({
  get: () => !!activeField.value?.extraInput,
  set: (val) => {}
})

// 常用正则表达式
const patternPresets = {
  phone: { pattern: '^1[3-9]\\d{9}$', message: '请输入正确的手机号' },
  email: { pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$', message: '请输入正确的邮箱地址' },
  idcard: { pattern: '^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$', message: '请输入正确的身份证号' },
  number: { pattern: '^\\d+$', message: '请输入纯数字' },
  chinese: { pattern: '^[\\u4e00-\\u9fa5]+$', message: '请输入中文' }
}

// 题型定义
const fieldTypes = [
  { type: 'radio', label: '单选题', icon: Select, defaultProps: {
    options: [
      { label: '选项1', value: 'A', score: 0, checked: false },
      { label: '选项2', value: 'B', score: 0, checked: false }
    ],
    hasOther: false,
    otherLabel: '其他',
    extraInput: null
  }},
  { type: 'checkbox', label: '多选题', icon: Checked, defaultProps: {
    options: [
      { label: '选项1', value: 'A', score: 0, checked: false },
      { label: '选项2', value: 'B', score: 0, checked: false }
    ],
    hasOther: false,
    otherLabel: '其他',
    minSelect: 0,
    maxSelect: 0,
    extraInput: null
  }},
  { type: 'text', label: '填空题', icon: Edit, defaultProps: {
    minLength: 0,
    maxLength: 200,
    pattern: '',
    patternMessage: '',
    inputType: 'text'
  }},
  { type: 'textarea', label: '多行文本', icon: Document, defaultProps: {
    minLength: 0,
    maxLength: 1000,
    rows: 4,
    pattern: '',
    patternMessage: ''
  }},
  { type: 'multi-input', label: '组合输入', icon: Grid, defaultProps: {
    subFields: [
      { id: `sub_${Date.now()}_1`, type: 'input', label: '字段1', inputType: 'text', required: true, placeholder: '' },
      { id: `sub_${Date.now()}_2`, type: 'input', label: '字段2', inputType: 'text', required: true, placeholder: '' }
    ]
  }},
  { type: 'select', label: '下拉选择', icon: Grid, defaultProps: {
    options: [
      { label: '选项1', value: 'opt1', score: 0, checked: false },
      { label: '选项2', value: 'opt2', score: 0, checked: false }
    ],
    hasOther: false
  }},
  { type: 'number', label: '数字题', icon: Plus, defaultProps: {
    min: 0,
    max: 100,
    step: 1,
    precision: 0
  }},
  { type: 'date', label: '日期题', icon: Calendar, defaultProps: {
    format: 'YYYY-MM-DD',
    minDate: null,
    maxDate: null
  }},
  { type: 'datetime', label: '日期时间', icon: Calendar, defaultProps: {
    format: 'YYYY-MM-DD HH:mm:ss'
  }},
  { type: 'rate', label: '评分题', icon: Star, defaultProps: {
    max: 5,
    allowHalf: true,
    showText: true,
    rateTexts: '极差,较差,一般,较好,极好'
  }},
  { type: 'file', label: '文件上传', icon: Upload, defaultProps: {
    maxFiles: 1,
    maxSize: 10,
    acceptTypes: ['*/*']
  }}
]

// 拖拽开始
function onDragStart(event, type) {
  event.dataTransfer.setData('fieldType', JSON.stringify(type))
}

// 放置
function onDrop(event) {
  const typeData = event.dataTransfer.getData('fieldType')
  if (!typeData) return

  const type = JSON.parse(typeData)
  addField(type)
}

// 添加字段
function addField(type) {
  const field = {
    id: Date.now().toString(),
    type: type.type,
    label: type.label,
    name: `field_${fields.value.length + 1}`,
    placeholder: '',
    required: false,
    helpText: '',
    defaultValue: '',
    width: 'full',
    hasCondition: false,
    conditionField: '',
    conditionOperator: 'eq',
    conditionValue: '',
    autoCalculate: false,
    calculateFormula: '',
    calculateFormat: 'decimal2',
    calculateTemplate: '',
    ...JSON.parse(JSON.stringify(type.defaultProps || {}))
  }

  fields.value.push(field)
  activeFieldId.value = field.id
}

// 选择字段
function selectField(field) {
  activeFieldId.value = field.id
}

// 移动字段
function moveField(index, direction) {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= fields.value.length) return

  const temp = fields.value[index]
  fields.value[index] = fields.value[newIndex]
  fields.value[newIndex] = temp
}

// 删除字段
function removeField(index) {
  fields.value.splice(index, 1)
  if (activeFieldId.value && !fields.value.find(f => f.id === activeFieldId.value)) {
    activeFieldId.value = null
  }
}

// 添加选项
function addOption() {
  if (!activeField.value || !activeField.value.options) return
  const optIndex = activeField.value.options.length + 1
  activeField.value.options.push({
    label: `选项${optIndex}`,
    value: `opt${optIndex}`,
    score: 0,
    checked: false
  })
}

// 删除选项
function removeOption(index) {
  if (!activeField.value || !activeField.value.options) return
  activeField.value.options.splice(index, 1)
}

// 切换其他选项
function toggleOtherOption(val) {
  if (val) {
    activeField.value.otherLabel = '其他'
  }
}

// 切换额外输入
function toggleExtraInput(val) {
  if (val) {
    activeField.value.extraInput = {
      id: `${activeField.value.id}_extra`,
      type: 'input',
      inputType: 'text',
      hint: '',
      placeholder: '',
      required: false,
      btnText: '点击上传',
      showWhen: ''
    }
  } else {
    activeField.value.extraInput = null
  }
}

// 添加子字段
function addSubField() {
  if (!activeField.value || activeField.value.type !== 'multi-input') return
  if (!activeField.value.subFields) {
    activeField.value.subFields = []
  }
  const subIndex = activeField.value.subFields.length + 1
  activeField.value.subFields.push({
    id: `${activeField.value.id}_${subIndex}`,
    type: 'input',
    label: `字段${subIndex}`,
    inputType: 'text',
    required: true,
    placeholder: '',
    readonly: false,
    hint: '',
    computeRule: '',
    validatorRule: null
  })
}

// 删除子字段
function removeSubField(index) {
  if (!activeField.value || activeField.value.type !== 'multi-input') return
  activeField.value.subFields.splice(index, 1)
}

// 获取子字段验证规则逻辑
function getSubFieldValidatorLogic(index) {
  if (!activeField.value?.subFields?.[index]?.validatorRule) {
    return ''
  }
  return activeField.value.subFields[index].validatorRule.logic || ''
}

// 设置子字段验证规则逻辑
function setSubFieldValidatorLogic(index, value) {
  if (!activeField.value?.subFields?.[index]) return
  if (!activeField.value.subFields[index].validatorRule) {
    activeField.value.subFields[index].validatorRule = { logic: '', message: '' }
  }
  activeField.value.subFields[index].validatorRule.logic = value
}

// 获取子字段验证规则消息
function getSubFieldValidatorMessage(index) {
  if (!activeField.value?.subFields?.[index]?.validatorRule) {
    return ''
  }
  return activeField.value.subFields[index].validatorRule.message || ''
}

// 设置子字段验证规则消息
function setSubFieldValidatorMessage(index, value) {
  if (!activeField.value?.subFields?.[index]) return
  if (!activeField.value.subFields[index].validatorRule) {
    activeField.value.subFields[index].validatorRule = { logic: '', message: '' }
  }
  activeField.value.subFields[index].validatorRule.message = value
}

// 应用预设正则
function applyPattern(type) {
  const preset = patternPresets[type]
  if (preset) {
    activeField.value.pattern = preset.pattern
    activeField.value.patternMessage = preset.message
  }
}

// 获取可作为条件的字段
function getConditionFields() {
  return fields.value.filter(f => f.id !== activeField.value?.id)
}

// 获取条件字段的类型
function getConditionFieldType() {
  const fieldName = activeField.value?.conditionField
  const field = fields.value.find(f => f.name === fieldName)
  return field?.type || ''
}

// 获取条件字段的选项
function getConditionFieldOptions() {
  const fieldName = activeField.value?.conditionField
  const field = fields.value.find(f => f.name === fieldName)
  return field?.options || []
}

// 判断是否需要条件值
function needsConditionValue() {
  const op = activeField.value?.conditionOperator
  return !['notEmpty', 'empty'].includes(op)
}

// 预览
function handlePreview() {
  previewData.value = {
    title: surveyTitle.value || '未命名问卷',
    description: surveyDesc.value,
    fields: JSON.parse(JSON.stringify(fields.value))
  }
  previewVisible.value = true
}

// 保存草稿
async function handleSaveDraft() {
  if (!surveyTitle.value) {
    ElMessage.warning('请输入问卷标题')
    return
  }

  const data = {
    title: surveyTitle.value,
    description: surveyDesc.value,
    schemaJson: { title: surveyTitle.value, description: surveyDesc.value, fields: fields.value },
    status: 0,
    // 新增字段
    maxSubmissions: enableSubmissionLimit.value ? maxSubmissions.value : 0,
    allowEdit: allowEdit.value,
    allowAnonymous: allowAnonymous.value,
    startTime: startTime.value,
    endTime: endTime.value
  }

  emit('save', data)
}

// 发布
async function handlePublish() {
  if (!surveyTitle.value) {
    ElMessage.warning('请输入问卷标题')
    return
  }

  if (fields.value.length === 0) {
    ElMessage.warning('请至少添加一道题目')
    return
  }

  await ElMessageBox.confirm('确定要发布该问卷吗？发布后用户即可填写', '发布确认')

  const data = {
    title: surveyTitle.value,
    description: surveyDesc.value,
    schemaJson: { title: surveyTitle.value, description: surveyDesc.value, fields: fields.value },
    status: 1,
    // 新增字段
    maxSubmissions: enableSubmissionLimit.value ? maxSubmissions.value : 0,
    allowEdit: allowEdit.value,
    allowAnonymous: allowAnonymous.value,
    startTime: startTime.value,
    endTime: endTime.value
  }

  emit('publish', data)
}

// 暴露方法
defineExpose({
  getData: () => ({
    title: surveyTitle.value,
    description: surveyDesc.value,
    schemaJson: { title: surveyTitle.value, description: surveyDesc.value, fields: fields.value },
    maxSubmissions: enableSubmissionLimit.value ? maxSubmissions.value : 0,
    allowEdit: allowEdit.value,
    allowAnonymous: allowAnonymous.value,
    startTime: startTime.value,
    endTime: endTime.value
  })
})

// 监听 initialData 变化，用于编辑模式异步加载
watch(() => props.initialData, (newData) => {
  if (newData) {
    surveyTitle.value = newData.title || ''
    surveyDesc.value = newData.description || ''
    if (newData.schemaJson?.fields) {
      fields.value = JSON.parse(JSON.stringify(newData.schemaJson.fields))
    }
    // 加载问卷设置
    if (newData.maxSubmissions && newData.maxSubmissions > 0) {
      enableSubmissionLimit.value = true
      maxSubmissions.value = newData.maxSubmissions
    } else {
      enableSubmissionLimit.value = false
    }
    allowEdit.value = newData.allowEdit || false
    allowAnonymous.value = newData.allowAnonymous !== false
    startTime.value = newData.startTime || null
    endTime.value = newData.endTime || null
  }
}, { immediate: true })
</script>

<style scoped lang="scss">
.survey-editor {
  display: flex;
  height: 100%;
  background: #f5f7fa;
  position: relative;
  overflow: hidden;
}

.field-panel {
  width: 160px;
  background: #fff;
  padding: 15px;
  border-right: 1px solid #e4e7ed;
  flex-shrink: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;

  h3 {
    margin: 0 0 12px;
    font-size: 14px;
    color: #606266;
    font-weight: 600;
  }

  .field-types {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .field-type-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 8px;
    cursor: grab;
    transition: all 0.2s;

    &:hover {
      background: #e6f0ff;
      transform: translateX(3px);
    }

    &:active {
      cursor: grabbing;
    }

    .field-icon {
      color: #409eff;
    }

    span {
      font-size: 13px;
      color: #606266;
    }
  }
}

.editor-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  display: flex;
  flex-direction: column;

  .editor-header {
    margin-bottom: 20px;
    flex-shrink: 0;

    .title-input {
      :deep(.el-input__wrapper) {
        font-size: 20px;
        font-weight: bold;
        padding: 15px;
      }
    }

    .desc-input {
      margin-top: 10px;
    }
  }

  .fields-container {
    flex: 1;
    min-height: 200px;
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    overflow-y: auto;
    scrollbar-width: thin;
  }

  .empty-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 400px;
    color: #c0c4cc;

    p {
      margin-top: 15px;
      font-size: 15px;
    }
  }

  .field-item {
    position: relative;
    padding: 20px;
    margin-bottom: 15px;
    background: #fafafa;
    border: 2px solid transparent;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #f0f7ff;
      border-color: #c0d9ff;
    }

    &.active {
      border-color: #409eff;
      background: #f0f7ff;
      box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
    }

    .field-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 15px;
      padding-bottom: 12px;
      border-bottom: 1px solid #e4e7ed;

      .field-info {
        flex: 1;
      }

      .field-title-row {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;

        .index-num {
          color: #409eff;
          font-weight: 600;
          font-size: 16px;
          min-width: 24px;
        }

        .field-title {
          font-weight: 600;
          color: #303133;
          font-size: 16px;
        }
      }

      .field-tags {
        display: flex;
        gap: 6px;
        flex-wrap: wrap;
        padding-left: 32px;
      }

      .field-actions {
        opacity: 0;
        transition: opacity 0.2s;
        flex-shrink: 0;
        display: flex;
        gap: 4px;
      }
    }

    &:hover .field-actions {
      opacity: 1;
    }

    .field-preview {
      padding: 15px;
      background: #fff;
      border-radius: 8px;
      border: 1px solid #ebeef5;
    }
  }
}

.property-panel {
  width: 340px;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  flex-shrink: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;

  h3 {
    margin: 0;
    padding: 15px 20px;
    font-size: 14px;
    color: #606266;
    font-weight: 600;
    border-bottom: 1px solid #e4e7ed;
    position: sticky;
    top: 0;
    background: #fff;
    z-index: 10;
  }

  .property-form {
    padding: 10px 15px;
  }

  :deep(.el-collapse-item__header) {
    font-weight: 500;
    color: #303133;
  }

  :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
    color: #606266;
    padding-bottom: 4px;
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-left: 10px;
  }

  .options-editor {
    .option-item {
      margin-bottom: 12px;
      padding: 10px;
      background: #f5f7fa;
      border-radius: 6px;

      .option-row {
        display: flex;
        align-items: center;
        gap: 8px;

        .default-check {
          flex-shrink: 0;
        }

        .option-label {
          flex: 1;
        }

        .option-score {
          width: 80px;
        }
      }

      .option-value {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 8px;
        font-size: 12px;
        color: #909399;

        .el-input {
          width: 120px;
        }
      }
    }

    .option-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px dashed #e4e7ed;
    }

    .select-limit {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 12px;
      padding: 10px;
      background: #fdf6ec;
      border-radius: 6px;
      font-size: 13px;
      color: #e6a23c;
    }
  }

  .validation-section {
    .validation-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 10px;
      font-size: 13px;
      color: #606266;

      .el-input {
        flex: 1;
      }
    }

    .validation-presets {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 10px;
      flex-wrap: wrap;

      span {
        font-size: 13px;
        color: #909399;
      }

      .el-tag {
        cursor: pointer;

        &:hover {
          background: #409eff;
          color: #fff;
        }
      }
    }
  }

  .condition-editor {
    .condition-desc {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px;
      background: #f4f4f5;
      border-radius: 6px;
      margin: 10px 0;
      font-size: 12px;
      color: #909399;
    }

    .condition-rule {
      padding: 10px;
      background: #f5f7fa;
      border-radius: 6px;

      .condition-row {
        margin-bottom: 10px;
        font-size: 13px;
        color: #606266;

        > span {
          display: block;
          margin-bottom: 6px;
        }
      }
    }
  }

  .calculate-editor {
    .calculate-desc {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px;
      background: #f0f9eb;
      border-radius: 6px;
      margin: 10px 0;
      font-size: 12px;
      color: #67c23a;
    }

    .calculate-rule {
      padding: 10px;
      background: #f5f7fa;
      border-radius: 6px;

      .calculate-row {
        margin-bottom: 10px;
        font-size: 13px;
        color: #606266;

        > span {
          display: block;
          margin-bottom: 6px;
        }
      }

      .formula-help {
        margin-top: 10px;
        padding: 10px;
        background: #fff;
        border-radius: 4px;
        font-size: 12px;
        color: #909399;

        p {
          margin: 4px 0;
        }
      }
    }
  }

  .subfields-editor {
    .subfield-item {
      margin-bottom: 15px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 8px;
      border: 1px solid #ebeef5;

      .subfield-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        padding-bottom: 8px;
        border-bottom: 1px dashed #dcdfe6;

        span {
          font-weight: 500;
          color: #303133;
        }
      }

      .subfield-body {
        :deep(.el-form-item) {
          margin-bottom: 10px;
        }
      }
    }
  }

  .extra-input-config {
    margin-top: 15px;
    padding-top: 15px;
    border-top: 1px dashed #e4e7ed;

    .extra-input-fields {
      margin-top: 12px;
      padding: 12px;
      background: #fdf6ec;
      border-radius: 6px;

      :deep(.el-form-item) {
        margin-bottom: 10px;
      }
    }
  }

  .option-hint-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
    font-size: 12px;
    color: #909399;

    .el-input {
      flex: 1;
    }
  }

  .compute-rule-editor {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed #e4e7ed;
  }
}

.editor-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 15px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  z-index: 100;
}

// 问卷设置面板样式
.survey-settings-panel {
  .submission-limit-setting {
    .el-checkbox {
      margin-bottom: 10px;
    }

    .el-input-number {
      margin-top: 8px;
    }
  }
}
</style>
