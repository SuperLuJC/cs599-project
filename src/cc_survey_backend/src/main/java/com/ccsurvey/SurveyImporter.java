package com.ccsurvey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.UUID;

/**
 * 问卷导入工具
 * 运行方式: java -cp target/classes:target/dependency/* com.ccsurvey.SurveyImporter
 */
public class SurveyImporter {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/cc_survey_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "123456";

    public static void main(String[] args) {
        String[] files = {
            "temp/企业调查问卷.json",
            "temp/成效测评表.json",
            "temp/第三份.json"
        };

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // 获取管理员ID
            Long adminId = getAdminId(conn);
            System.out.println("管理员ID: " + adminId);

            // 清理现有数据
            cleanData(conn);
            System.out.println("数据清理完成");

            // 导入问卷
            for (String file : files) {
                importSurvey(conn, file, adminId);
            }

            System.out.println("\n导入完成！共导入 " + files.length + " 份问卷");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Long getAdminId(Connection conn) throws SQLException {
        String sql = "SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        return 1L;
    }

    private static void cleanData(Connection conn) throws SQLException {
        // 删除答卷
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM survey_answer");
            System.out.println("已删除答卷数据");
        }

        // 删除问卷
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM survey_template");
            System.out.println("已删除问卷数据");
        }

        // 删除日志
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM sys_oper_log");
            System.out.println("已删除操作日志");
        }

        // 删除文件记录
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM sys_file");
            System.out.println("已删除文件记录");
        }
    }

    private static void importSurvey(Connection conn, String filePath, Long adminId) {
        try {
            // 读取JSON文件
            String basePath = "E:/workspace/mySubjects/survey_all/cc_survey/";
            String content = new String(Files.readAllBytes(Paths.get(basePath + filePath)), "UTF-8");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(content);

            String title = json.has("title") ? json.get("title").asText() : "未命名问卷";
            String formId = json.has("formId") ? json.get("formId").asText() : "";
            String uuid = UUID.randomUUID().toString().replace("-", "");

            // 插入数据库
            String sql = "INSERT INTO survey_template (uuid, title, description, form_id, schema_json, status, version, created_by, create_time, publish_time) " +
                        "VALUES (?, ?, '', ?, ?, 1, 1, ?, NOW(), NOW())";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid);
                pstmt.setString(2, title);
                pstmt.setString(3, formId);
                pstmt.setString(4, content); // schema_json
                pstmt.setLong(5, adminId);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("✓ 导入成功: " + title + " (formId: " + formId + ")");
                }
            }

        } catch (Exception e) {
            System.err.println("✗ 导入失败: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}
