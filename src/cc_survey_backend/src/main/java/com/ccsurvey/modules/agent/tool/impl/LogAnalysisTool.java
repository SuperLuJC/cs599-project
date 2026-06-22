package com.ccsurvey.modules.agent.tool.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsurvey.modules.agent.tool.AgentTool;
import com.ccsurvey.modules.agent.tool.ToolContext;
import com.ccsurvey.modules.agent.tool.ToolResult;
import com.ccsurvey.modules.log.entity.OperationLog;
import com.ccsurvey.modules.log.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 日志分析工具
 * 查询和分析系统操作日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogAnalysisTool extends AgentTool {

    private final OperationLogRepository logRepository;

    @Override
    public String getName() {
        return "analyze_logs";
    }

    @Override
    public String getDescription() {
        return """
                查询和分析系统操作日志。

                ## 查询类型 (query_type)
                - list: 日志列表
                - stats: 操作统计
                - by_module: 按模块查询
                - by_api: 按API路径查询

                ## 模块名称 (module)
                - 用户登录: 登录相关记录
                - 用户登出: 登出相关记录
                - 用户注册: 注册相关记录
                - 问卷管理: 问卷CRUD操作
                - 问卷提交: 问卷提交记录
                - 答卷管理: 答卷管理操作
                - 用户管理: 用户管理操作

                ## 操作类型 (operation_type)
                - LOGIN: 登录操作
                - LOGOUT: 登出操作
                - REGISTER: 注册操作
                - INSERT: 新增操作
                - UPDATE: 更新操作
                - DELETE: 删除操作
                - SELECT: 查询操作
                - EXPORT: 导出操作
                - OTHER: 其他操作

                ## 时间筛选
                - today: 今天
                - yesterday: 昨天
                - start_time / end_time: 自定义时间范围

                ## 示例调用
                - 查看登录记录: {"query_type": "list", "operation_type": "LOGIN"}
                - 查看今天的日志: {"query_type": "list", "time_range": "today"}
                - 查看问卷提交: {"query_type": "list", "module": "问卷提交"}
                - 统计登录情况: {"query_type": "stats", "operation_type": "LOGIN"}
                """;
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return createSchema(
                List.of(
                        Property.of("query_type", "string", "查询类型: list(列表), stats(统计), by_module(按模块), by_api(按API)"),
                        Property.of("module", "string", "模块名称: 用户登录, 用户登出, 用户注册, 问卷管理, 问卷提交, 答卷管理, 用户管理"),
                        Property.of("operation_type", "string", "操作类型: LOGIN, LOGOUT, REGISTER, INSERT, UPDATE, DELETE, SELECT, EXPORT, OTHER"),
                        Property.of("api_path", "string", "API路径筛选（如 login, submit, register）"),
                        Property.of("time_range", "string", "时间范围: today(今天), yesterday(昨天), week(本周)"),
                        Property.of("start_time", "string", "开始时间 (格式: yyyy-MM-dd HH:mm:ss)"),
                        Property.of("end_time", "string", "结束时间 (格式: yyyy-MM-dd HH:mm:ss)"),
                        Property.of("status", "string", "状态筛选: success(成功), fail(失败)"),
                        Property.of("limit", "integer", "返回记录数量限制 (默认50)")
                ),
                List.of("query_type")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, ToolContext context) {
        try {
            String queryType = (String) arguments.get("query_type");
            if (queryType == null || queryType.trim().isEmpty()) {
                queryType = "list";
            }

            int limit = arguments.get("limit") != null ? ((Number) arguments.get("limit")).intValue() : 50;

            log.info("LogAnalysisTool executing: queryType={}, arguments={}", queryType, arguments);

            return switch (queryType) {
                case "list" -> executeList(arguments, limit);
                case "stats" -> executeStats(arguments);
                case "by_module" -> executeByModule(arguments, limit);
                case "by_api" -> executeByApi(arguments, limit);
                default -> ToolResult.error("未知的查询类型: " + queryType + "。支持的类型: list, stats, by_module, by_api");
            };

        } catch (Exception e) {
            log.error("日志分析失败", e);
            return ToolResult.error("日志分析失败: " + e.getMessage());
        }
    }

    /**
     * 列表查询
     */
    private ToolResult executeList(Map<String, Object> arguments, int limit) {
        LambdaQueryWrapper<OperationLog> wrapper = buildQueryWrapper(arguments);
        wrapper.orderByDesc(OperationLog::getOperTime);
        wrapper.last("LIMIT " + limit);

        List<OperationLog> logs = logRepository.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (OperationLog logEntry : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("log_id", logEntry.getId());
            item.put("module", logEntry.getTitle());
            item.put("operation_type", logEntry.getBusinessType());
            item.put("api_path", extractApiPath(logEntry.getOperUrl()));
            item.put("user_name", logEntry.getOperName());
            item.put("oper_time", logEntry.getOperTime() != null ? logEntry.getOperTime().toString() : null);
            item.put("ip", logEntry.getOperIp());
            item.put("status", logEntry.getStatus() != null && logEntry.getStatus() == 0 ? "成功" : "失败");
            item.put("error_msg", logEntry.getErrorMsg());
            result.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total_count", logs.size());
        data.put("records", result);
        data.put("query_time", LocalDateTime.now().toString());

        return ToolResult.success(data);
    }

    /**
     * 统计分析
     */
    private ToolResult executeStats(Map<String, Object> arguments) {
        LambdaQueryWrapper<OperationLog> wrapper = buildQueryWrapper(arguments);

        List<OperationLog> logs = logRepository.selectList(wrapper);

        if (logs.isEmpty()) {
            return ToolResult.success(Map.of(
                    "message", "暂无符合条件的日志记录",
                    "total_count", 0
            ));
        }

        // 按模块统计
        Map<String, Integer> moduleCount = new LinkedHashMap<>();
        // 按操作类型统计
        Map<String, Integer> typeCount = new LinkedHashMap<>();
        // 按状态统计
        int successCount = 0;
        int failCount = 0;

        for (OperationLog logEntry : logs) {
            // 模块统计
            String module = logEntry.getTitle() != null ? logEntry.getTitle() : "未知模块";
            moduleCount.merge(module, 1, Integer::sum);

            // 操作类型统计
            String type = logEntry.getBusinessType() != null ? logEntry.getBusinessType() : "OTHER";
            typeCount.merge(type, 1, Integer::sum);

            // 状态统计 (status = 0 表示成功，status = 1 表示失败)
            if (logEntry.getStatus() != null && logEntry.getStatus() == 0) {
                successCount++;
            } else {
                failCount++;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_operations", logs.size());
        stats.put("success_count", successCount);
        stats.put("fail_count", failCount);
        stats.put("success_rate", logs.size() > 0 ? String.format("%.1f%%", (successCount * 100.0 / logs.size())) : "0%");
        stats.put("by_module", moduleCount);
        stats.put("by_operation_type", typeCount);

        return ToolResult.success(stats);
    }

    /**
     * 按模块查询
     */
    private ToolResult executeByModule(Map<String, Object> arguments, int limit) {
        String module = (String) arguments.get("module");
        if (module == null || module.trim().isEmpty()) {
            // 返回所有模块列表
            return ToolResult.success(Map.of(
                    "available_modules", List.of("用户登录", "用户登出", "用户注册", "问卷管理", "问卷提交", "答卷管理", "用户管理"),
                    "message", "请指定要查询的模块名称"
            ));
        }

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getTitle, module);
        applyTimeFilter(wrapper, arguments);
        applyStatusFilter(wrapper, arguments);
        wrapper.orderByDesc(OperationLog::getOperTime);
        wrapper.last("LIMIT " + limit);

        List<OperationLog> logs = logRepository.selectList(wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("module", module);
        data.put("total_count", logs.size());
        data.put("records", formatLogList(logs));

        return ToolResult.success(data);
    }

    /**
     * 按API路径查询
     */
    private ToolResult executeByApi(Map<String, Object> arguments, int limit) {
        String apiPath = (String) arguments.get("api_path");
        if (apiPath == null || apiPath.trim().isEmpty()) {
            return ToolResult.error("请指定要查询的API路径（如 login, submit, register）");
        }

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(OperationLog::getOperUrl, apiPath);
        applyTimeFilter(wrapper, arguments);
        applyStatusFilter(wrapper, arguments);
        wrapper.orderByDesc(OperationLog::getOperTime);
        wrapper.last("LIMIT " + limit);

        List<OperationLog> logs = logRepository.selectList(wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("api_path", apiPath);
        data.put("total_count", logs.size());
        data.put("records", formatLogList(logs));

        return ToolResult.success(data);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<OperationLog> buildQueryWrapper(Map<String, Object> arguments) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 模块筛选
        String module = (String) arguments.get("module");
        if (module != null && !module.trim().isEmpty()) {
            wrapper.eq(OperationLog::getTitle, module);
        }

        // 操作类型筛选
        String operationType = (String) arguments.get("operation_type");
        if (operationType != null && !operationType.trim().isEmpty()) {
            wrapper.eq(OperationLog::getBusinessType, operationType);
        }

        // API路径筛选
        String apiPath = (String) arguments.get("api_path");
        if (apiPath != null && !apiPath.trim().isEmpty()) {
            wrapper.like(OperationLog::getOperUrl, apiPath);
        }

        // 时间筛选
        applyTimeFilter(wrapper, arguments);

        // 状态筛选
        applyStatusFilter(wrapper, arguments);

        return wrapper;
    }

    /**
     * 应用时间筛选
     */
    private void applyTimeFilter(LambdaQueryWrapper<OperationLog> wrapper, Map<String, Object> arguments) {
        String timeRange = (String) arguments.get("time_range");

        if ("today".equals(timeRange)) {
            wrapper.ge(OperationLog::getOperTime, LocalDateTime.of(LocalDate.now(), LocalTime.MIN));
        } else if ("yesterday".equals(timeRange)) {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            wrapper.ge(OperationLog::getOperTime, LocalDateTime.of(yesterday, LocalTime.MIN));
            wrapper.lt(OperationLog::getOperTime, LocalDateTime.of(LocalDate.now(), LocalTime.MIN));
        } else if ("week".equals(timeRange)) {
            wrapper.ge(OperationLog::getOperTime, LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN));
        } else {
            // 自定义时间范围
            String startTime = (String) arguments.get("start_time");
            String endTime = (String) arguments.get("end_time");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            if (startTime != null && !startTime.trim().isEmpty()) {
                try {
                    wrapper.ge(OperationLog::getOperTime, LocalDateTime.parse(startTime, formatter));
                } catch (Exception ignored) {
                }
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                try {
                    wrapper.le(OperationLog::getOperTime, LocalDateTime.parse(endTime, formatter));
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 应用状态筛选
     */
    private void applyStatusFilter(LambdaQueryWrapper<OperationLog> wrapper, Map<String, Object> arguments) {
        String status = (String) arguments.get("status");
        if ("success".equals(status)) {
            wrapper.eq(OperationLog::getStatus, 0);
        } else if ("fail".equals(status)) {
            wrapper.eq(OperationLog::getStatus, 1);
        }
    }

    /**
     * 格式化日志列表
     */
    private List<Map<String, Object>> formatLogList(List<OperationLog> logs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OperationLog logEntry : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("log_id", logEntry.getId());
            item.put("module", logEntry.getTitle());
            item.put("operation_type", logEntry.getBusinessType());
            item.put("api_path", extractApiPath(logEntry.getOperUrl()));
            item.put("user_name", logEntry.getOperName());
            item.put("oper_time", logEntry.getOperTime() != null ? logEntry.getOperTime().toString() : null);
            item.put("ip", logEntry.getOperIp());
            item.put("status", logEntry.getStatus() != null && logEntry.getStatus() == 0 ? "成功" : "失败");
            item.put("error_msg", logEntry.getErrorMsg());
            result.add(item);
        }
        return result;
    }

    /**
     * 从完整URL中提取API路径
     */
    private String extractApiPath(String url) {
        if (url == null) return "";
        // 提取最后一个路径段
        String[] parts = url.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : url;
    }
}
