package com.ccsurvey.modules.test.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.modules.survey.entity.SurveyTemplate;
import com.ccsurvey.modules.survey.repository.SurveyTemplateRepository;
import com.ccsurvey.modules.user.entity.User;
import com.ccsurvey.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 测试数据生成控制器
 * 仅在test profile下启用
 */
@Slf4j
@RestController
@RequestMapping("/test/data")
@RequiredArgsConstructor
@Profile("test")
public class TestDataController {

    private final UserRepository userRepository;
    private final SurveyTemplateRepository templateRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 生成测试用户
     */
    @PostMapping("/users/generate")
    @Transactional
    public ApiResponse<Map<String, Object>> generateTestUsers(@RequestParam(defaultValue = "10") int count) {
        List<String> createdUuids = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setId(UUID.randomUUID().toString().replace("-", ""));
            user.setUsername("test_user_" + System.currentTimeMillis() + "_" + i);
            user.setPassword(passwordEncoder.encode("Test123456"));
            user.setEmail("test" + i + "@test.com");
            user.setName("测试用户" + i);
            user.setRole("user");
            user.setStatus(1);
            user.setEmailVerified(1);
            user.setLoginFailCount(0);

            userRepository.insert(user);
            createdUuids.add(user.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("uuids", createdUuids);

        log.info("生成测试用户: count={}", count);
        return ApiResponse.success(result);
    }

    /**
     * 生成测试问卷
     */
    @PostMapping("/surveys/generate")
    @Transactional
    public ApiResponse<Map<String, Object>> generateTestSurveys(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "true") boolean published) {

        List<String> createdUuids = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            SurveyTemplate template = new SurveyTemplate();
            template.setId(UUID.randomUUID().toString().replace("-", ""));
            template.setTitle("测试问卷_" + System.currentTimeMillis() + "_" + i);
            template.setDescription("这是一个测试问卷");
            template.setStatus(published ? 1 : 0);
            template.setVersion(1);
            template.setMaxSubmissions(0);
            template.setAllowEdit(0);
            template.setAllowAnonymous(0);

            // 简单的JSON Schema
            Map<String, Object> schema = new HashMap<>();
            schema.put("title", template.getTitle());

            List<Map<String, Object>> fields = new ArrayList<>();

            // 添加一个单选题
            Map<String, Object> radioField = new HashMap<>();
            radioField.put("id", "q1");
            radioField.put("type", "radio");
            radioField.put("label", "您对本次服务的满意度？");
            radioField.put("required", true);

            List<Map<String, Object>> options = new ArrayList<>();
            options.add(Map.of("label", "非常满意", "value", "A", "score", 5));
            options.add(Map.of("label", "满意", "value", "B", "score", 4));
            options.add(Map.of("label", "一般", "value", "C", "score", 3));
            options.add(Map.of("label", "不满意", "value", "D", "score", 2));
            options.add(Map.of("label", "非常不满意", "value", "E", "score", 1));
            radioField.put("options", options);

            fields.add(radioField);
            schema.put("fields", fields);

            template.setSchemaJson(schema);

            templateRepository.insert(template);
            createdUuids.add(template.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("uuids", createdUuids);
        result.put("published", published);

        log.info("生成测试问卷: count={}, published={}", count, published);
        return ApiResponse.success(result);
    }

    /**
     * 清理测试数据
     */
    @DeleteMapping("/clear")
    @Transactional
    public ApiResponse<Map<String, Object>> clearTestData() {
        // 删除测试用户
        long userCount = userRepository.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .likeRight(User::getUsername, "test_user_")
        );
        userRepository.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .likeRight(User::getUsername, "test_user_")
        );

        // 删除测试问卷
        long surveyCount = templateRepository.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SurveyTemplate>()
                        .likeRight(SurveyTemplate::getTitle, "测试问卷_")
        );
        templateRepository.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SurveyTemplate>()
                        .likeRight(SurveyTemplate::getTitle, "测试问卷_")
        );

        Map<String, Object> result = new HashMap<>();
        result.put("deletedUsers", userCount);
        result.put("deletedSurveys", surveyCount);

        log.info("清理测试数据: users={}, surveys={}", userCount, surveyCount);
        return ApiResponse.success(result);
    }
}