package com.ccsurvey.modules.agent.prompt;

/**
 * 问卷字段模板
 * 定义每种题型的完整配置规范
 */
public class FieldTemplates {

    /**
     * 字段类型说明
     */
    public static final String FIELD_TYPES = """
            ## 支持的字段类型

            | 类型 | 说明 | 必要属性 | 可选属性 |
            |------|------|----------|----------|
            | input | 单行文本输入 | name, label | placeholder, inputType, pattern, defaultValue |
            | radio | 单选题 | name, label, options | required, showWhen, extraInput |
            | checkbox | 多选题 | name, label, options | required, showWhen, extraInput |
            | select | 下拉选择 | name, label, options | required, placeholder, showWhen |
            | date | 日期选择 | name, label | dateType, format, placeholder, required |
            | upload | 文件上传 | name, label | accept, maxFiles, btnText, required |
            | multi-input | 多字段组合 | name, label, subFields | required |
            """;

    /**
     * 文本输入模板
     */
    public static final String INPUT_TEMPLATE = """
            ### 文本输入 (input)

            **基础模板：**
            ```json
            {
              "name": "username",
              "label": "您的姓名",
              "type": "input",
              "required": true,
              "placeholder": "请输入您的姓名"
            }
            ```

            **完整属性模板：**
            ```json
            {
              "name": "phone",
              "label": "手机号码",
              "type": "input",
              "required": true,
              "placeholder": "请输入11位手机号",
              "inputType": "text",
              "pattern": "^1[3-9]\\\\d{9}$",
              "errorMessage": "请输入正确的手机号格式",
              "defaultValue": "",
              "readonly": false,
              "showWhen": null,
              "computeRule": null
            }
            ```

            **属性说明：**
            - name: 字段标识（英文，唯一）【必填】
            - label: 显示标签（中文）【必填】
            - type: 固定为 "input"【必填】
            - required: 是否必填，默认 false【可选】
            - placeholder: 输入提示【可选】
            - inputType: 输入类型，"text" 或 "number"【可选】
            - pattern: 正则验证【可选】
            - errorMessage: 验证失败提示【可选】
            - defaultValue: 默认值【可选】
            - readonly: 是否只读【可选】
            - showWhen: 条件显示，如 "field1 === 'yes'"【可选】
            - computeRule: 自动计算公式【可选】
            """;

    /**
     * 单选题模板
     */
    public static final String RADIO_TEMPLATE = """
            ### 单选题 (radio)

            **基础模板：**
            ```json
            {
              "name": "gender",
              "label": "您的性别",
              "type": "radio",
              "required": true,
              "options": [
                {"label": "男", "value": "1"},
                {"label": "女", "value": "2"}
              ]
            }
            ```

            **带评分模板：**
            ```json
            {
              "name": "satisfaction",
              "label": "您对我们的服务满意吗？",
              "type": "radio",
              "required": true,
              "options": [
                {"label": "非常满意", "value": "5", "score": 5},
                {"label": "满意", "value": "4", "score": 4},
                {"label": "一般", "value": "3", "score": 3},
                {"label": "不满意", "value": "2", "score": 2},
                {"label": "非常不满意", "value": "1", "score": 1}
              ]
            }
            ```

            **带额外输入模板：**
            ```json
            {
              "name": "feedback_source",
              "label": "您是从哪里了解到我们的？",
              "type": "radio",
              "required": true,
              "options": [
                {"label": "朋友推荐", "value": "friend"},
                {"label": "网络搜索", "value": "search"},
                {"label": "社交媒体", "value": "social"},
                {"label": "其他", "value": "other"}
              ],
              "extraInput": {
                "id": "feedback_source_other",
                "hint": "请具体说明",
                "type": "input",
                "placeholder": "请输入来源",
                "showWhen": "feedback_source === 'other'"
              }
            }
            ```

            **带条件显示模板：**
            ```json
            {
              "name": "travel_plan",
              "label": "您是否计划五一出行？",
              "type": "radio",
              "required": true,
              "options": [
                {"label": "是", "value": "yes"},
                {"label": "否", "value": "no"},
                {"label": "尚未确定", "value": "undecided"}
              ],
              "showWhen": null
            }
            ```

            **options 属性说明：**
            - label: 选项显示文本【必填】
            - value: 选项值（唯一）【必填】
            - score: 评分值（用于自动计分）【可选】
            - hint: 选项提示说明【可选】

            **extraInput 属性说明：**
            - id: 额外输入字段标识【必填】
            - hint: 输入提示【必填】
            - type: 输入类型 "input" 或 "upload"【必填】
            - placeholder: 输入框提示【可选】
            - showWhen: 显示条件【可选】
            """;

    /**
     * 多选题模板
     */
    public static final String CHECKBOX_TEMPLATE = """
            ### 多选题 (checkbox)

            **基础模板：**
            ```json
            {
              "name": "interests",
              "label": "您的兴趣爱好（可多选）",
              "type": "checkbox",
              "required": false,
              "options": [
                {"label": "阅读", "value": "reading"},
                {"label": "运动", "value": "sports"},
                {"label": "音乐", "value": "music"},
                {"label": "旅行", "value": "travel"}
              ]
            }
            ```

            **带评分模板：**
            ```json
            {
              "name": "services_used",
              "label": "您使用过我们的哪些服务？（可多选）",
              "type": "checkbox",
              "required": false,
              "options": [
                {"label": "在线咨询", "value": "consult", "score": 2},
                {"label": "产品购买", "value": "purchase", "score": 3},
                {"label": "售后服务", "value": "service", "score": 2},
                {"label": "会员服务", "value": "member", "score": 5}
              ]
            }
            ```

            **带额外输入模板：**
            ```json
            {
              "name": "improvement_areas",
              "label": "您认为需要改进的方面（可多选）",
              "type": "checkbox",
              "required": false,
              "options": [
                {"label": "产品质量", "value": "quality"},
                {"label": "服务态度", "value": "service"},
                {"label": "价格策略", "value": "price"},
                {"label": "其他", "value": "other"}
              ],
              "extraInput": {
                "id": "improvement_other",
                "hint": "请具体说明需要改进的方面",
                "type": "input",
                "placeholder": "请输入",
                "showWhen": "improvement_areas && improvement_areas.includes('other')"
              }
            }
            ```

            **注意：** 多选题的 value 存储为数组，showWhen 条件判断需使用 includes() 方法
            """;

    /**
     * 下拉选择模板
     */
    public static final String SELECT_TEMPLATE = """
            ### 下拉选择 (select)

            **基础模板：**
            ```json
            {
              "name": "city",
              "label": "您所在的城市",
              "type": "select",
              "required": true,
              "placeholder": "请选择城市",
              "options": [
                {"label": "北京", "value": "beijing"},
                {"label": "上海", "value": "shanghai"},
                {"label": "广州", "value": "guangzhou"},
                {"label": "深圳", "value": "shenzhen"}
              ]
            }
            ```

            **年龄段选择模板：**
            ```json
            {
              "name": "age_group",
              "label": "您的年龄段",
              "type": "select",
              "required": true,
              "placeholder": "请选择",
              "options": [
                {"label": "18岁以下", "value": "under18"},
                {"label": "18-25岁", "value": "18-25"},
                {"label": "26-35岁", "value": "26-35"},
                {"label": "36-45岁", "value": "36-45"},
                {"label": "46-55岁", "value": "46-55"},
                {"label": "55岁以上", "value": "over55"}
              ]
            }
            ```

            **预算范围模板：**
            ```json
            {
              "name": "budget",
              "label": "您的预算范围",
              "type": "select",
              "required": false,
              "placeholder": "请选择预算范围",
              "options": [
                {"label": "1000元以下", "value": "under1000"},
                {"label": "1000-3000元", "value": "1000-3000"},
                {"label": "3000-5000元", "value": "3000-5000"},
                {"label": "5000-10000元", "value": "5000-10000"},
                {"label": "10000元以上", "value": "over10000"}
              ]
            }
            ```
            """;

    /**
     * 日期选择模板
     */
    public static final String DATE_TEMPLATE = """
            ### 日期选择 (date)

            **基础模板：**
            ```json
            {
              "name": "birth_date",
              "label": "出生日期",
              "type": "date",
              "required": true,
              "placeholder": "请选择日期"
            }
            ```

            **完整属性模板：**
            ```json
            {
              "name": "appointment_date",
              "label": "预约日期",
              "type": "date",
              "required": true,
              "dateType": "date",
              "format": "YYYY-MM-DD",
              "placeholder": "请选择预约日期"
            }
            ```

            **日期时间选择：**
            ```json
            {
              "name": "meeting_time",
              "label": "会议时间",
              "type": "date",
              "required": true,
              "dateType": "datetime",
              "format": "YYYY-MM-DD HH:mm",
              "placeholder": "请选择会议时间"
            }
            ```

            **属性说明：**
            - dateType: 日期类型，"date" 或 "datetime"【可选，默认 date】
            - format: 显示格式【可选，默认 YYYY-MM-DD】
            """;

    /**
     * 文件上传模板
     */
    public static final String UPLOAD_TEMPLATE = """
            ### 文件上传 (upload)

            **基础模板：**
            ```json
            {
              "name": "attachment",
              "label": "上传附件",
              "type": "upload",
              "required": false,
              "accept": ".pdf,.doc,.docx",
              "maxFiles": 1,
              "btnText": "点击上传"
            }
            ```

            **图片上传模板：**
            ```json
            {
              "name": "photo",
              "label": "上传照片",
              "type": "upload",
              "required": true,
              "accept": "image/jpeg,image/png",
              "maxFiles": 3,
              "btnText": "点击或拖拽上传照片"
            }
            ```

            **属性说明：**
            - accept: 允许的文件类型，如 ".pdf,.doc" 或 "image/*"【可选】
            - maxFiles: 最大上传数量【可选，默认 1】
            - btnText: 上传按钮文字【可选】
            """;

    /**
     * 多字段组合模板
     */
    public static final String MULTI_INPUT_TEMPLATE = """
            ### 多字段组合 (multi-input)

            用于将多个相关字段组合在一起显示。

            **基础模板：**
            ```json
            {
              "name": "contact_info",
              "label": "联系方式",
              "type": "multi-input",
              "required": true,
              "subFields": [
                {
                  "id": "contact_name",
                  "label": "联系人姓名",
                  "type": "input",
                  "required": true,
                  "placeholder": "请输入姓名"
                },
                {
                  "id": "contact_phone",
                  "label": "联系电话",
                  "type": "input",
                  "required": true,
                  "placeholder": "请输入电话"
                },
                {
                  "id": "contact_email",
                  "label": "电子邮箱",
                  "type": "input",
                  "required": false,
                  "placeholder": "请输入邮箱"
                }
              ]
            }
            ```

            **地址信息模板：**
            ```json
            {
              "name": "address",
              "label": "收货地址",
              "type": "multi-input",
              "required": true,
              "subFields": [
                {
                  "id": "province",
                  "label": "省份",
                  "type": "input",
                  "required": true,
                  "placeholder": "请输入省份"
                },
                {
                  "id": "city",
                  "label": "城市",
                  "type": "input",
                  "required": true,
                  "placeholder": "请输入城市"
                },
                {
                  "id": "detail_address",
                  "label": "详细地址",
                  "type": "input",
                  "required": true,
                  "placeholder": "请输入详细地址"
                }
              ]
            }
            ```

            **subFields 属性说明：**
            - id: 子字段标识【必填】
            - label: 子字段标签【必填】
            - type: 子字段类型，支持 input、upload【必填】
            - required: 是否必填【可选】
            - placeholder: 输入提示【可选】
            - hint: 字段说明【可选】
            - readonly: 是否只读【可选】
            - computeRule: 自动计算公式【可选】
            """;

    /**
     * 条件显示说明
     */
    public static final String SHOW_WHEN_GUIDE = """
            ## 条件显示 (showWhen)

            使用 JavaScript 表达式控制字段是否显示。

            **示例：**

            1. 简单条件：
            ```json
            {"showWhen": "gender === 'female'"}
            ```

            2. 多选题条件（使用 includes）：
            ```json
            {"showWhen": "interests && interests.includes('sports')"}
            ```

            3. 复合条件：
            ```json
            {"showWhen": "age_group === 'adult' && gender === 'male'"}
            ```

            4. 数值比较：
            ```json
            {"showWhen": "score > 60"}
            ```
            """;

    /**
     * 自动计算说明
     */
    public static final String COMPUTE_RULE_GUIDE = """
            ## 自动计算 (computeRule)

            使用 JavaScript 表达式自动计算字段值。

            **示例：**

            1. 简单求和：
            ```json
            {"computeRule": "field1 + field2 + field3"}
            ```

            2. 条件计算：
            ```json
            {"computeRule": "base_score * (bonus === 'yes' ? 1.2 : 1)"}
            ```

            3. 平均值：
            ```json
            {"computeRule": "(score1 + score2 + score3) / 3"}
            ```

            **注意：** 计算字段会自动设置为只读，用户无法手动输入。
            """;

    /**
     * 完整问卷示例
     */
    public static final String FULL_SURVEY_EXAMPLE = """
            ## 完整问卷示例

            ```json
            {
              "title": "客户满意度调查问卷",
              "description": "感谢您参与本次调查，您的反馈对我们非常重要！",
              "fields": [
                {
                  "name": "name",
                  "label": "您的姓名",
                  "type": "input",
                  "required": true,
                  "placeholder": "请输入姓名"
                },
                {
                  "name": "phone",
                  "label": "联系电话",
                  "type": "input",
                  "required": true,
                  "placeholder": "请输入手机号",
                  "pattern": "^1[3-9]\\\\d{9}$",
                  "errorMessage": "请输入正确的手机号"
                },
                {
                  "name": "age_group",
                  "label": "您的年龄段",
                  "type": "select",
                  "required": true,
                  "placeholder": "请选择",
                  "options": [
                    {"label": "18岁以下", "value": "under18"},
                    {"label": "18-35岁", "value": "18-35"},
                    {"label": "36-55岁", "value": "36-55"},
                    {"label": "55岁以上", "value": "over55"}
                  ]
                },
                {
                  "name": "satisfaction",
                  "label": "您对我们服务的整体满意度",
                  "type": "radio",
                  "required": true,
                  "options": [
                    {"label": "非常满意", "value": "5", "score": 5},
                    {"label": "满意", "value": "4", "score": 4},
                    {"label": "一般", "value": "3", "score": 3},
                    {"label": "不满意", "value": "2", "score": 2},
                    {"label": "非常不满意", "value": "1", "score": 1}
                  ]
                },
                {
                  "name": "services_used",
                  "label": "您使用过我们的哪些服务",
                  "type": "checkbox",
                  "required": false,
                  "options": [
                    {"label": "产品咨询", "value": "consult"},
                    {"label": "售后服务", "value": "service"},
                    {"label": "技术支持", "value": "support"}
                  ]
                },
                {
                  "name": "suggestions",
                  "label": "您的宝贵建议",
                  "type": "input",
                  "required": false,
                  "placeholder": "请输入您的建议"
                }
              ]
            }
            ```
            """;

    /**
     * 获取完整模板文档
     */
    public static String getFullTemplate() {
        return FIELD_TYPES + "\n\n" +
               INPUT_TEMPLATE + "\n\n" +
               RADIO_TEMPLATE + "\n\n" +
               CHECKBOX_TEMPLATE + "\n\n" +
               SELECT_TEMPLATE + "\n\n" +
               DATE_TEMPLATE + "\n\n" +
               UPLOAD_TEMPLATE + "\n\n" +
               MULTI_INPUT_TEMPLATE + "\n\n" +
               SHOW_WHEN_GUIDE + "\n\n" +
               COMPUTE_RULE_GUIDE + "\n\n" +
               FULL_SURVEY_EXAMPLE;
    }
}
