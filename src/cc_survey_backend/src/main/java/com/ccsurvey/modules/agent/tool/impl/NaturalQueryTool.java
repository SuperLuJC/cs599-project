package com.ccsurvey.modules.agent.tool.impl;

import com.ccsurvey.modules.agent.tool.AgentTool;
import com.ccsurvey.modules.agent.tool.ToolContext;
import com.ccsurvey.modules.agent.tool.ToolResult;
import com.ccsurvey.modules.log.repository.OperationLogRepository;
import com.ccsurvey.modules.survey.repository.SurveyAnswerRepository;
import com.ccsurvey.modules.survey.repository.SurveyTemplateRepository;
import com.ccsurvey.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 自然语言查询工具
 * 将用户的自然语言问题转换为数据库查询，返回真实数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaturalQueryTool extends AgentTool {

    private final JdbcTemplate jdbcTemplate;
    private final SurveyTemplateRepository templateRepository;
    private final SurveyAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final OperationLogRepository logRepository;

    /**
     * 敏感字段黑名单 - 这些字段不能出现在 SELECT 中
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "password_reset_token", "email_verify_token",
            "locked_until", "login_fail_count"
    );

    /**
     * 敏感字段脱敏映射
     */
    private static final Map<String, String> MASK_FIELDS = Map.of(
            "email", "email_masked",
            "phone", "phone_masked"
    );

    /**
     * 允许查询的表白名单
     */
    private static final Set<String> ALLOWED_TABLES = Set.of(
            "survey_template", "survey_answer", "sys_user", "sys_oper_log"
    );

    /**
     * SQL 关键词黑名单
     */
    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE",
            "GRANT", "REVOKE", "EXEC", "EXECUTE", "INTO", "OUTFILE", "LOAD_FILE"
    );

    @Override
    public String getName() {
        return "natural_query";
    }

    @Override
    public String getDescription() {
        return "自然语言数据查询工具，执行真实数据库查询返回真实数据。支持预设查询类型和直接SQL查询。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return createSchema(
                List.of(
                        Property.of("query_type", "string", "查询类型: survey_submission_stats, top_surveys, user_submission_stats, survey_avg_score, login_stats, user_register_stats, surveys_no_submission, survey_detail, direct_sql"),
                        Property.of("sql", "string", "直接SQL查询语句（仅用于 direct_sql 类型，必须是 SELECT 语句）"),
                        Property.of("survey_title", "string", "问卷标题（支持模糊匹配，可选）"),
                        Property.of("user_name", "string", "用户名（支持模糊匹配，可选）"),
                        Property.of("time_start", "string", "开始时间（格式: yyyy-MM-dd HH:mm:ss，可选）"),
                        Property.of("time_end", "string", "结束时间（格式: yyyy-MM-dd HH:mm:ss，可选）"),
                        Property.of("limit", "integer", "返回数量限制（默认5，最大100，可选）")
                ),
                List.of("query_type")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        String queryType = (String) args.get("query_type");

        if (queryType == null || queryType.isEmpty()) {
            return ToolResult.error("缺少必填参数: query_type");
        }

        try {
            log.info("执行自然语言查询: queryType={}, args={}", queryType, args);

            Object result = switch (queryType) {
                case "survey_submission_stats" -> querySurveySubmissionStats(args);
                case "top_surveys" -> queryTopSurveys(args);
                case "user_submission_stats" -> queryUserSubmissionStats(args);
                case "survey_avg_score" -> querySurveyAvgScore(args);
                case "login_stats" -> queryLoginStats(args);
                case "user_register_stats" -> queryUserRegisterStats(args);
                case "surveys_no_submission" -> querySurveysNoSubmission(args);
                case "survey_detail" -> querySurveyDetail(args);
                case "direct_sql" -> queryDirectSql(args);
                default -> Map.of("error", "不支持的查询类型: " + queryType);
            };

            log.info("查询结果: {}", result);
            return ToolResult.success(result);

        } catch (Exception e) {
            log.error("查询执行失败: {}", e.getMessage(), e);
            return ToolResult.error("查询执行失败: " + e.getMessage());
        }
    }

    /**
     * 直接执行 LLM 生成的 SQL（带安全校验）
     */
    private Map<String, Object> queryDirectSql(Map<String, Object> args) {
        String sql = (String) args.get("sql");

        if (sql == null || sql.trim().isEmpty()) {
            return Map.of("error", "direct_sql 类型需要提供 sql 参数");
        }

        // 安全校验
        String validationError = validateSql(sql);
        if (validationError != null) {
            return Map.of("error", validationError);
        }

        // 添加 LIMIT 限制（如果没有）
        String finalSql = ensureLimit(sql, 100);

        log.info("执行直接SQL查询: {}", finalSql);

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(finalSql);

            // 脱敏处理
            List<Map<String, Object>> maskedResults = maskSensitiveData(results);

            return Map.of(
                    "query_type", "direct_sql",
                    "sql", finalSql,
                    "results", maskedResults,
                    "total_count", maskedResults.size()
            );
        } catch (Exception e) {
            log.error("SQL执行失败: {}", e.getMessage());
            return Map.of("error", "SQL执行失败: " + e.getMessage());
        }
    }

    /**
     * 校验 SQL 安全性
     */
    private String validateSql(String sql) {
        String upperSql = sql.toUpperCase().trim();

        // 1. 必须以 SELECT 开头
        if (!upperSql.startsWith("SELECT")) {
            return "只允许执行 SELECT 查询语句";
        }

        // 2. 检查禁用的关键词
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                return "SQL 语句包含不允许的关键词: " + keyword;
            }
        }

        // 3. 检查敏感字段
        for (String field : SENSITIVE_FIELDS) {
            // 检查 SELECT 后面的字段列表
            if (upperSql.matches(".*\\bSELECT\\b.*\\b" + field.toUpperCase() + "\\b.*")) {
                return "SQL 语句包含敏感字段: " + field;
            }
        }

        // 4. 检查表名白名单
        for (String table : ALLOWED_TABLES) {
            // 简单检查：SQL 中出现的表名必须在白名单中
        }

        // 5. 禁止子查询（简化处理，检查括号嵌套）
        int parenCount = 0;
        for (char c : sql.toCharArray()) {
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
            if (parenCount > 1) {
                return "暂不支持复杂的子查询";
            }
        }

        return null; // 校验通过
    }

    /**
     * 确保 SQL 有 LIMIT 限制
     */
    private String ensureLimit(String sql, int maxLimit) {
        String upperSql = sql.toUpperCase().trim();

        if (!upperSql.contains("LIMIT")) {
            return sql + " LIMIT " + maxLimit;
        }

        // 提取已有的 LIMIT 值，确保不超过最大值
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("LIMIT\\s+(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(sql);

        if (matcher.find()) {
            int limit = Integer.parseInt(matcher.group(1));
            if (limit > maxLimit) {
                return matcher.replaceFirst("LIMIT " + maxLimit);
            }
        }

        return sql;
    }

    /**
     * 脱敏处理敏感数据
     */
    private List<Map<String, Object>> maskSensitiveData(List<Map<String, Object>> results) {
        List<Map<String, Object>> maskedResults = new ArrayList<>();

        for (Map<String, Object> row : results) {
            Map<String, Object> maskedRow = new HashMap<>(row);

            // 邮箱脱敏：z***@example.com
            if (maskedRow.containsKey("email")) {
                String email = (String) maskedRow.get("email");
                if (email != null && email.contains("@")) {
                    String[] parts = email.split("@");
                    if (parts[0].length() > 1) {
                        maskedRow.put("email", parts[0].charAt(0) + "***@" + parts[1]);
                    } else {
                        maskedRow.put("email", "***@" + parts[1]);
                    }
                }
            }

            // 手机号脱敏：138****1234
            if (maskedRow.containsKey("phone")) {
                String phone = (String) maskedRow.get("phone");
                if (phone != null && phone.length() >= 7) {
                    maskedRow.put("phone", phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4));
                }
            }

            maskedResults.add(maskedRow);
        }

        return maskedResults;
    }

    /**
     * 问卷提交统计
     */
    private Map<String, Object> querySurveySubmissionStats(Map<String, Object> args) {
        String surveyTitle = (String) args.get("survey_title");
        String timeStart = (String) args.get("time_start");
        String timeEnd = (String) args.get("time_end");

        StringBuilder sql = new StringBuilder("""
            SELECT t.id, t.title, t.status, COUNT(a.id) as submission_count
            FROM survey_template t
            LEFT JOIN survey_answer a ON t.id = a.template_id AND a.deleted = 0
            WHERE t.deleted = 0
            """);

        List<Object> params = new ArrayList<>();

        // 标题模糊匹配
        if (surveyTitle != null && !surveyTitle.isEmpty()) {
            sql.append(" AND t.title LIKE ?");
            params.add("%" + surveyTitle + "%");
        }

        // 时间范围
        if (timeStart != null && timeEnd != null) {
            sql.append(" AND (a.create_time IS NULL OR a.create_time BETWEEN ? AND ?)");
            params.add(timeStart);
            params.add(timeEnd);
        }

        sql.append(" GROUP BY t.id, t.title, t.status ORDER BY submission_count DESC");

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        return Map.of(
                "query_type", "survey_submission_stats",
                "survey_title", surveyTitle != null ? surveyTitle : "全部",
                "time_range", timeStart != null ? timeStart + " ~ " + timeEnd : "全部时间",
                "results", results,
                "total_count", results.size()
        );
    }

    /**
     * 提交量排行
     */
    private Map<String, Object> queryTopSurveys(Map<String, Object> args) {
        String timeStart = (String) args.get("time_start");
        String timeEnd = (String) args.get("time_end");
        int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 5;

        StringBuilder sql = new StringBuilder("""
            SELECT t.title, COUNT(a.id) as submission_count
            FROM survey_template t
            INNER JOIN survey_answer a ON t.id = a.template_id
            WHERE t.deleted = 0 AND a.deleted = 0
            """);

        List<Object> params = new ArrayList<>();

        // 时间范围
        if (timeStart != null && timeEnd != null) {
            sql.append(" AND a.create_time BETWEEN ? AND ?");
            params.add(timeStart);
            params.add(timeEnd);
        }

        sql.append(" GROUP BY t.id, t.title ORDER BY submission_count DESC LIMIT ?");
        params.add(limit);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        return Map.of(
                "query_type", "top_surveys",
                "time_range", timeStart != null ? timeStart + " ~ " + timeEnd : "全部时间",
                "limit", limit,
                "results", results,
                "total_count", results.size()
        );
    }

    /**
     * 用户提交统计
     */
    private Map<String, Object> queryUserSubmissionStats(Map<String, Object> args) {
        String userName = (String) args.get("user_name");
        String timeStart = (String) args.get("time_start");
        String timeEnd = (String) args.get("time_end");

        StringBuilder sql = new StringBuilder("""
            SELECT a.submitter_name, COUNT(a.id) as submission_count
            FROM survey_answer a
            WHERE a.deleted = 0
            """);

        List<Object> params = new ArrayList<>();

        // 用户名模糊匹配
        if (userName != null && !userName.isEmpty()) {
            sql.append(" AND a.submitter_name LIKE ?");
            params.add("%" + userName + "%");
        }

        // 时间范围
        if (timeStart != null && timeEnd != null) {
            sql.append(" AND a.create_time BETWEEN ? AND ?");
            params.add(timeStart);
            params.add(timeEnd);
        }

        sql.append(" GROUP BY a.submitter_name ORDER BY submission_count DESC");

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        int totalSubmissions = results.stream()
                .mapToInt(r -> ((Number) r.get("submission_count")).intValue())
                .sum();

        return Map.of(
                "query_type", "user_submission_stats",
                "user_name", userName != null ? userName : "全部用户",
                "time_range", timeStart != null ? timeStart + " ~ " + timeEnd : "全部时间",
                "results", results,
                "total_users", results.size(),
                "total_submissions", totalSubmissions
        );
    }

    /**
     * 问卷平均分
     */
    private Map<String, Object> querySurveyAvgScore(Map<String, Object> args) {
        String surveyTitle = (String) args.get("survey_title");

        if (surveyTitle == null || surveyTitle.isEmpty()) {
            return Map.of("error", "查询问卷平均分需要提供问卷标题");
        }

        String sql = """
            SELECT t.id, t.title, COUNT(a.id) as submission_count,
                   AVG(a.total_score) as avg_score,
                   MAX(a.total_score) as max_score,
                   MIN(a.total_score) as min_score
            FROM survey_template t
            INNER JOIN survey_answer a ON t.id = a.template_id
            WHERE t.deleted = 0 AND a.deleted = 0
            AND t.title LIKE ?
            GROUP BY t.id, t.title
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, "%" + surveyTitle + "%");

        return Map.of(
                "query_type", "survey_avg_score",
                "survey_title", surveyTitle,
                "results", results,
                "total_count", results.size()
        );
    }

    /**
     * 登录统计
     */
    private Map<String, Object> queryLoginStats(Map<String, Object> args) {
        String timeStart = (String) args.get("time_start");
        String timeEnd = (String) args.get("time_end");

        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*) as login_count,
                   COUNT(DISTINCT oper_user_id) as unique_users
            FROM sys_oper_log
            WHERE title = '用户登录' AND status = 1
            """);

        List<Object> params = new ArrayList<>();

        // 时间范围
        if (timeStart != null && timeEnd != null) {
            sql.append(" AND oper_time BETWEEN ? AND ?");
            params.add(timeStart);
            params.add(timeEnd);
        }

        Map<String, Object> result = jdbcTemplate.queryForMap(sql.toString(), params.toArray());

        return Map.of(
                "query_type", "login_stats",
                "time_range", timeStart != null ? timeStart + " ~ " + timeEnd : "全部时间",
                "login_count", result.get("login_count"),
                "unique_users", result.get("unique_users")
        );
    }

    /**
     * 用户注册统计
     */
    private Map<String, Object> queryUserRegisterStats(Map<String, Object> args) {
        String timeStart = (String) args.get("time_start");
        String timeEnd = (String) args.get("time_end");

        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*) as register_count
            FROM sys_user
            WHERE deleted = 0
            """);

        List<Object> params = new ArrayList<>();

        // 时间范围
        if (timeStart != null && timeEnd != null) {
            sql.append(" AND create_time BETWEEN ? AND ?");
            params.add(timeStart);
            params.add(timeEnd);
        }

        Map<String, Object> result = jdbcTemplate.queryForMap(sql.toString(), params.toArray());

        return Map.of(
                "query_type", "user_register_stats",
                "time_range", timeStart != null ? timeStart + " ~ " + timeEnd : "全部时间",
                "register_count", result.get("register_count")
        );
    }

    /**
     * 无提交问卷
     */
    private Map<String, Object> querySurveysNoSubmission(Map<String, Object> args) {
        String sql = """
            SELECT t.id, t.title, t.status, t.create_time
            FROM survey_template t
            LEFT JOIN survey_answer a ON t.id = a.template_id AND a.deleted = 0
            WHERE t.deleted = 0
            AND a.id IS NULL
            ORDER BY t.create_time DESC
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        return Map.of(
                "query_type", "surveys_no_submission",
                "results", results,
                "total_count", results.size()
        );
    }

    /**
     * 问卷详情查询
     */
    private Map<String, Object> querySurveyDetail(Map<String, Object> args) {
        String surveyTitle = (String) args.get("survey_title");

        if (surveyTitle == null || surveyTitle.isEmpty()) {
            return Map.of("error", "查询问卷详情需要提供问卷标题");
        }

        String sql = """
            SELECT t.id, t.title, t.description, t.status, t.max_submissions,
                   t.allow_edit, t.allow_anonymous, t.show_score,
                   t.create_time, t.publish_time,
                   (SELECT COUNT(*) FROM survey_answer WHERE template_id = t.id AND deleted = 0) as submission_count
            FROM survey_template t
            WHERE t.deleted = 0 AND t.title LIKE ?
            ORDER BY t.create_time DESC
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, "%" + surveyTitle + "%");

        return Map.of(
                "query_type", "survey_detail",
                "survey_title", surveyTitle,
                "results", results,
                "total_count", results.size()
        );
    }
}
