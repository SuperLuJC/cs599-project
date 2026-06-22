package com.ccsurvey.modules.agent.tool.impl;

import com.ccsurvey.modules.agent.tool.AgentTool;
import com.ccsurvey.modules.agent.tool.ToolContext;
import com.ccsurvey.modules.agent.tool.ToolResult;
import com.ccsurvey.modules.survey.entity.SurveyTemplate;
import com.ccsurvey.modules.survey.repository.SurveyTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 问卷构建工具
 * 通过自然语言创建问卷
 *
 * 流程：
 * 1. preview - 生成预览计划，不保存
 * 2. confirm - 用户确认后保存草稿
 * 3. update - 更新现有问卷
 */
@Component
@RequiredArgsConstructor
public class SurveyBuildTool extends AgentTool {

    private final SurveyTemplateRepository templateRepository;

    @Override
    public String getName() {
        return "build_survey";
    }

    @Override
    public String getDescription() {
        return """
                问卷构建工具。支持以下操作：
                - preview: 根据用户描述生成问卷计划预览，不保存数据库
                - confirm: 用户确认后，保存问卷为草稿（需要提供 preview_id）
                - update: 更新现有问卷

                重要：创建问卷必须先 preview，用户确认后再 confirm，不能直接创建。
                """;
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return createSchema(
                List.of(
                        Property.of("action", "string", "操作类型: preview(预览计划), confirm(确认保存), update(更新问卷)"),
                        Property.of("preview_id", "string", "预览ID (confirm 时必填，从 preview 结果获取)"),
                        Property.of("survey_uuid", "string", "问卷UUID (update 时必填)"),
                        Property.of("title", "string", "问卷标题"),
                        Property.of("description", "string", "问卷描述"),
                        Property.of("fields", "array", "字段列表，每个字段包含 type, label, name, options(可选), required(可选) 等"),
                        Property.of("show_score", "boolean", "是否向用户展示评分结果（默认 false 不展示，仅显示感谢信息）"),
                        Property.of("max_submissions", "integer", "最大提交份数（0表示不限制，默认0）"),
                        Property.of("allow_edit", "boolean", "是否允许用户修改提交（默认 false）"),
                        Property.of("allow_anonymous", "boolean", "是否允许匿名提交（默认 false）"),
                        Property.of("start_time", "string", "问卷开始时间（格式：yyyy-MM-dd HH:mm:ss，可选）"),
                        Property.of("end_time", "string", "问卷结束时间（格式：yyyy-MM-dd HH:mm:ss，可选）")
                ),
                List.of("action")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, ToolContext context) {
        try {
            String action = (String) arguments.get("action");
            if (action == null) {
                return ToolResult.error("action 参数不能为空");
            }

            // 确保 arguments 是可修改的 Map（从 JSON 解析的 Map 可能不可修改）
            Map<String, Object> modifiableArgs = new LinkedHashMap<>(arguments);

            // 处理 fields 参数：可能是字符串（JSON）或 List
            Object fieldsObj = modifiableArgs.get("fields");
            if (fieldsObj instanceof String) {
                String fieldsStr = (String) fieldsObj;
                if (fieldsStr.trim().isEmpty()) {
                    return ToolResult.error("fields 参数为空字符串");
                }
                try {
                    // 使用 FastJSON 直接解析为可修改的 List
                    List<Map<String, Object>> parsedFields = new ArrayList<>();
                    com.alibaba.fastjson2.JSONArray jsonArray = com.alibaba.fastjson2.JSON.parseArray(fieldsStr);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        Object item = jsonArray.get(i);
                        if (item instanceof Map) {
                            // 创建新的可修改 Map
                            Map<String, Object> fieldMap = new LinkedHashMap<>((Map<String, Object>) item);
                            parsedFields.add(fieldMap);
                        }
                    }
                    modifiableArgs.put("fields", parsedFields);
                } catch (Exception e) {
                    return ToolResult.error("fields 参数格式错误，无法解析为字段列表: " + e.getMessage());
                }
            } else if (fieldsObj instanceof List) {
                // 如果已经是 List，确保其中的 Map 是可修改的
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> originalFields = (List<Map<String, Object>>) fieldsObj;
                List<Map<String, Object>> modifiableFields = new ArrayList<>();
                for (Map<String, Object> field : originalFields) {
                    modifiableFields.add(new LinkedHashMap<>(field));
                }
                modifiableArgs.put("fields", modifiableFields);
            }

            return switch (action) {
                case "preview" -> executePreview(modifiableArgs, context);
                case "confirm" -> executeConfirm(modifiableArgs, context);
                case "update" -> executeUpdate(modifiableArgs, context);
                default -> ToolResult.error("未知的操作类型: " + action + "。支持的操作: preview, confirm, update");
            };

        } catch (Exception e) {
            // 打印完整堆栈跟踪以便调试
            e.printStackTrace();
            String stackTrace = java.util.Arrays.toString(e.getStackTrace());
            return ToolResult.error("问卷构建失败: " + e.getClass().getSimpleName() + " - " + e.getMessage() + " at " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0] : "unknown"));
        }
    }

    /**
     * 预览模式：生成计划但不保存
     */
    private ToolResult executePreview(Map<String, Object> arguments, ToolContext context) {
        String title = (String) arguments.get("title");
        if (title == null || title.trim().isEmpty()) {
            return ToolResult.error("问卷标题不能为空");
        }

        String description = (String) arguments.getOrDefault("description", "");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) arguments.get("fields");
        if (fields == null || fields.isEmpty()) {
            return ToolResult.error("问卷字段不能为空");
        }

        // 获取是否展示评分（默认 false）
        Boolean showScore = getBooleanArg(arguments, "show_score", false);

        // 获取提交限制参数
        Integer maxSubmissions = getIntegerArg(arguments, "max_submissions", 0);
        Boolean allowEdit = getBooleanArg(arguments, "allow_edit", false);
        Boolean allowAnonymous = getBooleanArg(arguments, "allow_anonymous", false);

        // 获取时间参数
        String startTime = (String) arguments.get("start_time");
        String endTime = (String) arguments.get("end_time");

        // 规范化字段格式（特别是 options 格式）
        List<Map<String, Object>> normalizedFields = normalizeFields(fields);

        // 生成预览ID（用于后续确认）
        String previewId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 构建 schema_json 预览
        Map<String, Object> schemaJson = new LinkedHashMap<>();
        schemaJson.put("title", title);
        schemaJson.put("description", description);
        schemaJson.put("fields", normalizedFields);

        // 存储预览数据到会话上下文
        Map<String, Object> previewData = new LinkedHashMap<>();
        previewData.put("title", title);
        previewData.put("description", description);
        previewData.put("schemaJson", schemaJson);
        previewData.put("fields", normalizedFields);
        previewData.put("showScore", showScore);
        previewData.put("maxSubmissions", maxSubmissions);
        previewData.put("allowEdit", allowEdit);
        previewData.put("allowAnonymous", allowAnonymous);
        previewData.put("startTime", startTime);
        previewData.put("endTime", endTime);
        previewData.put("createdAt", System.currentTimeMillis());

        // 保存到上下文，有效期5分钟
        Map<String, Object> existingPreviews = context.getContext("pendingPreviews");
        Map<String, Object> pendingPreviews;
        if (existingPreviews == null || existingPreviews.isEmpty()) {
            pendingPreviews = new HashMap<>();
        } else {
            pendingPreviews = new HashMap<>(existingPreviews);
        }
        pendingPreviews.put(previewId, previewData);
        context.setContext("pendingPreviews", pendingPreviews);

        // 构建预览结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("preview_id", previewId);
        result.put("title", title);
        result.put("description", description);
        result.put("field_count", fields.size());
        result.put("fields", fields);
        result.put("show_score", showScore);
        result.put("max_submissions", maxSubmissions);
        result.put("allow_edit", allowEdit);
        result.put("allow_anonymous", allowAnonymous);
        result.put("start_time", startTime);
        result.put("end_time", endTime);
        result.put("schema_preview", schemaJson);
        result.put("status", "preview");
        result.put("message", "问卷计划已生成，等待用户确认");
        result.put("next_action", "confirm");
        result.put("next_action_hint", "用户确认后，请调用 build_survey 工具，参数: action='confirm', preview_id='" + previewId + "'");

        // 构建摘要信息
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("问卷计划已生成：\n- 标题：%s\n- 字段数：%d\n- 展示评分：%s",
                title, fields.size(), showScore ? "是" : "否"));
        if (maxSubmissions > 0) {
            summary.append("\n- 最大提交份数：").append(maxSubmissions);
        }
        if (allowEdit) {
            summary.append("\n- 允许修改提交：是");
        }
        if (allowAnonymous) {
            summary.append("\n- 允许匿名提交：是");
        }
        if (startTime != null) {
            summary.append("\n- 开始时间：").append(startTime);
        }
        if (endTime != null) {
            summary.append("\n- 结束时间：").append(endTime);
        }
        summary.append("\n- 预览ID：").append(previewId);
        summary.append("\n\n请询问用户是否确认保存。如果用户确认，请执行 confirm 操作并传递 preview_id='").append(previewId).append("'。");

        // 返回需要确认的结果
        return ToolResult.needsConfirmation(summary.toString(), result);
    }

    /**
     * 确认模式：用户确认后保存草稿
     */
    private ToolResult executeConfirm(Map<String, Object> arguments, ToolContext context) {
        String previewId = (String) arguments.get("preview_id");
        if (previewId == null || previewId.isEmpty()) {
            return ToolResult.error("确认保存需要提供 preview_id（从预览结果获取）");
        }

        // 从上下文获取预览数据
        @SuppressWarnings("unchecked")
        Map<String, Object> pendingPreviews = context.getContext("pendingPreviews");
        if (pendingPreviews == null || !pendingPreviews.containsKey(previewId)) {
            return ToolResult.error("预览不存在或已过期，请重新生成计划");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> previewData = (Map<String, Object>) pendingPreviews.get(previewId);

        // 检查预览是否过期（5分钟）
        Long createdAt = (Long) previewData.get("createdAt");
        if (createdAt != null && System.currentTimeMillis() - createdAt > 5 * 60 * 1000) {
            pendingPreviews.remove(previewId);
            return ToolResult.error("预览已过期（超过5分钟），请重新生成计划");
        }

        String title = (String) previewData.get("title");
        String description = (String) previewData.get("description");
        @SuppressWarnings("unchecked")
        Map<String, Object> schemaJson = (Map<String, Object>) previewData.get("schemaJson");

        // 获取各项设置参数
        Boolean showScore = getBooleanArg(previewData, "showScore", false);
        Integer maxSubmissions = getIntegerArg(previewData, "maxSubmissions", 0);
        Boolean allowEdit = getBooleanArg(previewData, "allowEdit", false);
        Boolean allowAnonymous = getBooleanArg(previewData, "allowAnonymous", false);
        String startTimeStr = (String) previewData.get("startTime");
        String endTimeStr = (String) previewData.get("endTime");

        // 检查是否已存在相同标题的草稿（防止重复创建）
        List<SurveyTemplate> existingDrafts = templateRepository.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SurveyTemplate>()
                        .eq(SurveyTemplate::getTitle, title)
                        .eq(SurveyTemplate::getStatus, 0) // 草稿状态
                        .eq(SurveyTemplate::getCreatedBy, context.getUserId())
        );

        if (!existingDrafts.isEmpty()) {
            // 已存在同名草稿，提示用户
            SurveyTemplate existing = existingDrafts.get(0);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("existing_survey_uuid", existing.getId());
            result.put("title", title);
            result.put("message", "已存在同名草稿问卷，如需创建新问卷请修改标题，或更新现有问卷");
            return ToolResult.error("已存在标题为「" + title + "」的草稿问卷，请修改标题或更新现有问卷");
        }

        // 创建问卷
        SurveyTemplate template = new SurveyTemplate();
        template.setTitle(title);
        template.setDescription(description);
        template.setSchemaJson(schemaJson);
        template.setStatus(0); // 草稿
        template.setVersion(1);
        template.setMaxSubmissions(maxSubmissions);
        template.setAllowEdit(allowEdit ? 1 : 0);
        template.setAllowAnonymous(allowAnonymous ? 1 : 0);
        template.setShowScore(showScore ? 1 : 0);
        template.setCreatedBy(context.getUserId());

        // 解析时间
        if (startTimeStr != null && !startTimeStr.isEmpty()) {
            template.setStartTime(parseDateTime(startTimeStr));
        }
        if (endTimeStr != null && !endTimeStr.isEmpty()) {
            template.setEndTime(parseDateTime(endTimeStr));
        }

        templateRepository.insert(template);

        // 清除预览数据
        pendingPreviews.remove(previewId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("survey_uuid", template.getId());
        result.put("title", template.getTitle());
        result.put("status", "draft");
        result.put("show_score", showScore);
        result.put("max_submissions", maxSubmissions);
        result.put("allow_edit", allowEdit);
        result.put("allow_anonymous", allowAnonymous);
        result.put("start_time", startTimeStr);
        result.put("end_time", endTimeStr);
        result.put("message", "问卷已保存为草稿");

        return ToolResult.success(result);
    }

    /**
     * 更新模式：更新现有问卷
     */
    private ToolResult executeUpdate(Map<String, Object> arguments, ToolContext context) {
        String surveyUuid = (String) arguments.get("survey_uuid");
        if (surveyUuid == null || surveyUuid.isEmpty()) {
            return ToolResult.error("更新问卷需要提供 survey_uuid");
        }

        SurveyTemplate template = templateRepository.selectById(surveyUuid);
        if (template == null) {
            return ToolResult.error("问卷不存在: " + surveyUuid);
        }

        if (template.isPublished()) {
            return ToolResult.error("已发布的问卷不能修改");
        }

        String title = (String) arguments.get("title");
        String description = (String) arguments.getOrDefault("description", "");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) arguments.get("fields");

        // 构建 schema_json
        Map<String, Object> schemaJson = new LinkedHashMap<>();
        schemaJson.put("title", title != null ? title : template.getTitle());
        schemaJson.put("description", description);
        if (fields != null && !fields.isEmpty()) {
            schemaJson.put("fields", normalizeFields(fields));
        } else if (template.getSchemaJson() != null) {
            schemaJson.put("fields", template.getSchemaJson().get("fields"));
        }

        // 更新基本字段
        if (title != null) {
            template.setTitle(title);
        }
        template.setDescription(description);
        template.setSchemaJson(schemaJson);

        // 更新设置参数
        if (arguments.containsKey("show_score")) {
            template.setShowScore(getBooleanArg(arguments, "show_score", false) ? 1 : 0);
        }
        if (arguments.containsKey("max_submissions")) {
            template.setMaxSubmissions(getIntegerArg(arguments, "max_submissions", 0));
        }
        if (arguments.containsKey("allow_edit")) {
            template.setAllowEdit(getBooleanArg(arguments, "allow_edit", false) ? 1 : 0);
        }
        if (arguments.containsKey("allow_anonymous")) {
            template.setAllowAnonymous(getBooleanArg(arguments, "allow_anonymous", false) ? 1 : 0);
        }
        if (arguments.containsKey("start_time")) {
            String startTime = (String) arguments.get("start_time");
            if (startTime != null && !startTime.isEmpty()) {
                template.setStartTime(parseDateTime(startTime));
            } else {
                template.setStartTime(null);
            }
        }
        if (arguments.containsKey("end_time")) {
            String endTime = (String) arguments.get("end_time");
            if (endTime != null && !endTime.isEmpty()) {
                template.setEndTime(parseDateTime(endTime));
            } else {
                template.setEndTime(null);
            }
        }

        templateRepository.updateById(template);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("survey_uuid", template.getId());
        result.put("title", template.getTitle());
        result.put("max_submissions", template.getMaxSubmissions());
        result.put("allow_edit", template.getAllowEdit() == 1);
        result.put("allow_anonymous", template.getAllowAnonymous() == 1);
        result.put("start_time", template.getStartTime() != null ? template.getStartTime().toString() : null);
        result.put("end_time", template.getEndTime() != null ? template.getEndTime().toString() : null);
        result.put("message", "问卷更新成功");

        return ToolResult.success(result);
    }

    /**
     * 规范化字段格式
     * 将 options 从字符串数组转换为 { label, value } 格式的对象数组
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeFields(List<Map<String, Object>> fields) {
        List<Map<String, Object>> normalized = new ArrayList<>();

        for (Map<String, Object> field : fields) {
            Map<String, Object> normalizedField = new LinkedHashMap<>(field);

            // 处理 options 字段
            Object optionsObj = field.get("options");
            if (optionsObj != null) {
                List<Map<String, Object>> normalizedOptions = new ArrayList<>();

                if (optionsObj instanceof List) {
                    List<?> options = (List<?>) optionsObj;
                    for (int i = 0; i < options.size(); i++) {
                        Object opt = options.get(i);
                        if (opt instanceof String) {
                            // 字符串转换为 { label, value }
                            Map<String, Object> option = new LinkedHashMap<>();
                            option.put("label", opt);
                            option.put("value", String.valueOf(i + 1));
                            normalizedOptions.add(option);
                        } else if (opt instanceof Map) {
                            // 已经是对象格式，确保有 label 和 value
                            Map<String, Object> option = new LinkedHashMap<>((Map<String, Object>) opt);
                            if (!option.containsKey("label")) {
                                option.put("label", option.get("text"));
                            }
                            if (!option.containsKey("value")) {
                                option.put("value", String.valueOf(i + 1));
                            }
                            normalizedOptions.add(option);
                        }
                    }
                }

                normalizedField.put("options", normalizedOptions);
            }

            // 确保必要字段存在
            if (!normalizedField.containsKey("name")) {
                normalizedField.put("name", "field_" + System.nanoTime());
            }
            if (!normalizedField.containsKey("label")) {
                normalizedField.put("label", normalizedField.getOrDefault("name", "字段"));
            }
            if (!normalizedField.containsKey("type")) {
                normalizedField.put("type", "input");
            }
            if (!normalizedField.containsKey("required")) {
                normalizedField.put("required", false);
            }

            normalized.add(normalizedField);
        }

        return normalized;
    }

    /**
     * 获取布尔类型参数
     */
    private Boolean getBooleanArg(Map<String, Object> args, String key, Boolean defaultValue) {
        Object value = args.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * 获取整数类型参数
     */
    private Integer getIntegerArg(Map<String, Object> args, String key, Integer defaultValue) {
        Object value = args.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 解析日期时间字符串
     */
    private java.time.LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            return null;
        }
        try {
            // 尝试 ISO 格式
            return java.time.LocalDateTime.parse(dateTime, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e1) {
            try {
                // 尝试标准格式
                return java.time.LocalDateTime.parse(dateTime, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
