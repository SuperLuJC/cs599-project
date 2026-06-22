package com.ccsurvey.modules.test.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.modules.survey.dto.SurveyCreateRequest;
import com.ccsurvey.modules.survey.dto.SurveyTemplateDTO;
import com.ccsurvey.modules.survey.service.SurveyService;
import com.ccsurvey.modules.submission.dto.SubmitRequest;
import com.ccsurvey.modules.submission.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 问卷流程测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/test/survey")
@RequiredArgsConstructor
@Profile({"dev", "test"})
public class SurveyFlowTestController {

    private final SurveyService surveyService;
    private final SubmissionService submissionService;

    /**
     * 生成测试问卷Schema
     */
    @GetMapping("/schema/sample")
    public ApiResponse<Map<String, Object>> getSampleSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("title", "员工满意度调查问卷");
        schema.put("description", "请认真填写以下问卷，您的反馈对我们非常重要");

        List<Map<String, Object>> fields = new ArrayList<>();

        // 1. 单选题 - 带分数
        Map<String, Object> radioField = new LinkedHashMap<>();
        radioField.put("id", "q1");
        radioField.put("name", "satisfaction");
        radioField.put("type", "radio");
        radioField.put("label", "您对公司的整体满意度如何？");
        radioField.put("required", true);
        radioField.put("placeholder", "");
        radioField.put("helpText", "请选择最符合您感受的选项");
        radioField.put("options", Arrays.asList(
                createOption("非常满意", "opt1", 5),
                createOption("比较满意", "opt2", 4),
                createOption("一般", "opt3", 3),
                createOption("不太满意", "opt4", 2),
                createOption("非常不满意", "opt5", 1)
        ));
        radioField.put("hasOther", false);
        fields.add(radioField);

        // 2. 单选题 - 带"其他"选项
        Map<String, Object> radioWithOtherField = new LinkedHashMap<>();
        radioWithOtherField.put("id", "q2");
        radioWithOtherField.put("name", "department");
        radioWithOtherField.put("type", "radio");
        radioWithOtherField.put("label", "您所在的部门是？");
        radioWithOtherField.put("required", true);
        radioWithOtherField.put("options", Arrays.asList(
                createOption("技术部", "tech", 0),
                createOption("市场部", "marketing", 0),
                createOption("人事部", "hr", 0),
                createOption("财务部", "finance", 0)
        ));
        radioWithOtherField.put("hasOther", true);
        radioWithOtherField.put("otherLabel", "其他部门");
        fields.add(radioWithOtherField);

        // 3. 多选题 - 带选择限制
        Map<String, Object> checkboxField = new LinkedHashMap<>();
        checkboxField.put("id", "q3");
        checkboxField.put("name", "benefits");
        checkboxField.put("type", "checkbox");
        checkboxField.put("label", "您最看重公司的哪些福利？（最多选3项）");
        checkboxField.put("required", true);
        checkboxField.put("minSelect", 1);
        checkboxField.put("maxSelect", 3);
        checkboxField.put("options", Arrays.asList(
                createOption("五险一金", "insurance", 0),
                createOption("带薪年假", "vacation", 0),
                createOption("节日福利", "holiday", 0),
                createOption("培训机会", "training", 0),
                createOption("弹性工作", "flexible", 0)
        ));
        checkboxField.put("hasOther", true);
        fields.add(checkboxField);

        // 4. 填空题 - 带长度限制和正则验证
        Map<String, Object> textField = new LinkedHashMap<>();
        textField.put("id", "q4");
        textField.put("name", "suggestions");
        textField.put("type", "text");
        textField.put("label", "您对公司有什么建议？");
        textField.put("required", false);
        textField.put("placeholder", "请输入您的建议");
        textField.put("minLength", 10);
        textField.put("maxLength", 500);
        fields.add(textField);

        // 5. 多行文本
        Map<String, Object> textareaField = new LinkedHashMap<>();
        textareaField.put("id", "q5");
        textareaField.put("name", "feedback");
        textareaField.put("type", "textarea");
        textareaField.put("label", "请详细描述您的工作体验");
        textareaField.put("required", false);
        textareaField.put("placeholder", "请输入...");
        textareaField.put("rows", 4);
        textareaField.put("maxLength", 1000);
        fields.add(textareaField);

        // 6. 数字题
        Map<String, Object> numberField = new LinkedHashMap<>();
        numberField.put("id", "q6");
        numberField.put("name", "workYears");
        numberField.put("type", "number");
        numberField.put("label", "您在公司工作了多少年？");
        numberField.put("required", true);
        numberField.put("min", 0);
        numberField.put("max", 50);
        numberField.put("step", 1);
        numberField.put("precision", 0);
        fields.add(numberField);

        // 7. 下拉选择
        Map<String, Object> selectField = new LinkedHashMap<>();
        selectField.put("id", "q7");
        selectField.put("name", "position");
        selectField.put("type", "select");
        selectField.put("label", "您的职位级别是？");
        selectField.put("required", true);
        selectField.put("options", Arrays.asList(
                createOption("初级", "junior", 0),
                createOption("中级", "mid", 0),
                createOption("高级", "senior", 0),
                createOption("专家", "expert", 0)
        ));
        fields.add(selectField);

        // 8. 日期题
        Map<String, Object> dateField = new LinkedHashMap<>();
        dateField.put("id", "q8");
        dateField.put("name", "joinDate");
        dateField.put("type", "date");
        dateField.put("label", "您的入职日期是？");
        dateField.put("required", true);
        dateField.put("format", "YYYY-MM-DD");
        fields.add(dateField);

        // 9. 评分题
        Map<String, Object> rateField = new LinkedHashMap<>();
        rateField.put("id", "q9");
        rateField.put("name", "teamRate");
        rateField.put("type", "rate");
        rateField.put("label", "请为您的团队协作打分");
        rateField.put("required", true);
        rateField.put("max", 5);
        rateField.put("allowHalf", true);
        rateField.put("showText", true);
        rateField.put("texts", Arrays.asList("极差", "较差", "一般", "较好", "极好"));
        fields.add(rateField);

        // 10. 条件显示题 - 当q1选择"不太满意"或"非常不满意"时显示
        Map<String, Object> conditionalField = new LinkedHashMap<>();
        conditionalField.put("id", "q10");
        conditionalField.put("name", "improvement");
        conditionalField.put("type", "textarea");
        conditionalField.put("label", "您认为公司哪些方面需要改进？");
        conditionalField.put("required", false);
        conditionalField.put("hasCondition", true);
        conditionalField.put("conditionField", "satisfaction");
        conditionalField.put("conditionOperator", "contains");
        conditionalField.put("conditionValue", "opt4,opt5");
        conditionalField.put("maxLength", 500);
        fields.add(conditionalField);

        schema.put("fields", fields);

        return ApiResponse.success(schema);
    }

    /**
     * 创建完整测试问卷
     */
    @PostMapping("/create-full-test")
    public ApiResponse<SurveyTemplateDTO> createFullTestSurvey(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        if (userId == null) {
            userId = "test-user-uuid"; // 默认测试用户
            username = "testUser";
        }

        SurveyCreateRequest createRequest = new SurveyCreateRequest();
        createRequest.setTitle("完整功能测试问卷");
        createRequest.setDescription("此问卷包含所有题型和高级功能，用于测试系统完整性");

        // 构建Schema
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("title", createRequest.getTitle());
        schema.put("description", createRequest.getDescription());

        List<Map<String, Object>> fields = new ArrayList<>();

        // 添加各种题型
        fields.add(createRadioField("q1", "单选题-基本", Arrays.asList(
                createOption("选项A", "a", 1),
                createOption("选项B", "b", 2),
                createOption("选项C", "c", 3)
        ), true));

        fields.add(createRadioWithOtherField("q2", "单选题-带其他", Arrays.asList(
                createOption("选项1", "1", 0),
                createOption("选项2", "2", 0)
        ), true));

        fields.add(createCheckboxField("q3", "多选题", Arrays.asList(
                createOption("选项A", "a", 1),
                createOption("选项B", "b", 2),
                createOption("选项C", "c", 3)
        ), true, 1, 2));

        fields.add(createTextField("q4", "填空题", false, 0, 100));

        fields.add(createTextareaField("q5", "多行文本", false, 500));

        fields.add(createNumberField("q6", "数字题", true, 0, 100, 1));

        fields.add(createSelectField("q7", "下拉选择", Arrays.asList(
                createOption("选项1", "1", 0),
                createOption("选项2", "2", 0),
                createOption("选项3", "3", 0)
        ), true));

        fields.add(createDateField("q8", "日期题", true));

        fields.add(createRateField("q9", "评分题", true, 5));

        schema.put("fields", fields);
        createRequest.setSchemaJson(schema);

        SurveyTemplateDTO survey = surveyService.createSurvey(createRequest, userId, username);

        // 自动发布
        surveyService.publishSurvey(survey.getUuid());

        log.info("创建测试问卷成功: uuid={}", survey.getUuid());
        return ApiResponse.success("测试问卷创建成功", survey);
    }

    /**
     * 测试提交流程
     */
    @PostMapping("/submit-test/{uuid}")
    public ApiResponse<Map<String, Object>> testSubmit(
            @PathVariable String uuid,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        if (userId == null) {
            userId = "test-user-uuid";
            username = "testUser";
        }

        // 构建测试答案
        Map<String, Object> answerData = new LinkedHashMap<>();
        answerData.put("q1", "b");
        answerData.put("q2", Map.of("type", "other", "value", "自定义选项内容"));
        answerData.put("q3", Arrays.asList("a", "b"));
        answerData.put("q4", "这是一段测试文本");
        answerData.put("q5", "这是多行文本的内容\n第二行\n第三行");
        answerData.put("q6", 50);
        answerData.put("q7", "2");
        answerData.put("q8", "2024-01-15");
        answerData.put("q9", 4.5);

        SubmitRequest submitRequest = new SubmitRequest();
        submitRequest.setTemplateUuid(uuid);
        submitRequest.setData(answerData);

        Map<String, Object> result = submissionService.submit(submitRequest, userId, username, request);

        log.info("测试提交成功: uuid={}, result={}", uuid, result);
        return ApiResponse.success("提交测试成功", result);
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> createOption(String label, String value, int score) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("label", label);
        option.put("value", value);
        option.put("score", score);
        option.put("checked", false);
        return option;
    }

    private Map<String, Object> createRadioField(String name, String label, List<Map<String, Object>> options, boolean required) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", name);
        field.put("name", name);
        field.put("type", "radio");
        field.put("label", label);
        field.put("required", required);
        field.put("options", options);
        field.put("hasOther", false);
        return field;
    }

    private Map<String, Object> createRadioWithOtherField(String name, String label, List<Map<String, Object>> options, boolean required) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", name);
        field.put("name", name);
        field.put("type", "radio");
        field.put("label", label);
        field.put("required", required);
        field.put("options", options);
        field.put("hasOther", true);
        field.put("otherLabel", "其他");
        return field;
    }

    private Map<String, Object> createCheckboxField(String name, String label, List<Map<String, Object>> options, boolean required, int minSelect, int maxSelect) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", name);
        field.put("name", name);
        field.put("type", "checkbox");
        field.put("label", label);
        field.put("required", required);
        field.put("options", options);
        field.put("minSelect", minSelect);
        field.put("maxSelect", maxSelect);
        field.put("hasOther", false);
        return field;
    }

    private Map<String, Object> createTextField(String name, String label, boolean required, int minLength, int maxLength) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", name);
        field.put("name", name);
        field.put("type", "text");
        field.put("label", label);
        field.put("required", required);
        field.put("minLength", minLength);
        field.put("maxLength", maxLength);
        field.put("placeholder", "请输入");
        return field;
    }

    private Map<String, Object> createTextareaField(String name, String label, boolean required, int maxLength) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", name);
        field.put("name", name);
        field.put("type", "textarea");
        field.put("label", label);
        field.put("required", required);
        field.put("maxLength", maxLength);
        field.put("rows", 4);
        field.put("placeholder", "请输入");
        return field;
    }

    private Map<String, Object> createNumberField(String name, String label, boolean required, int min, int max, int step) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", name);
        field.put("name", name);
        field.put("type", "number");
        field.put("label", label);
        field.put("required", required);
        field.put("min", min);
        field.put("max", max);
        field.put("step", step);
        field.put("precision", 0);
        return field;
    }

    private Map<String, Object> createSelectField(String name, String label, List<Map<String, Object>> options, boolean required) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", name);
        field.put("name", name);
        field.put("type", "select");
        field.put("label", label);
        field.put("required", required);
        field.put("options", options);
        return field;
    }

    private Map<String, Object> createDateField(String name, String label, boolean required) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", name);
        field.put("name", name);
        field.put("type", "date");
        field.put("label", label);
        field.put("required", required);
        field.put("format", "YYYY-MM-DD");
        return field;
    }

    private Map<String, Object> createRateField(String name, String label, boolean required, int max) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", name);
        field.put("name", name);
        field.put("type", "rate");
        field.put("label", label);
        field.put("required", required);
        field.put("max", max);
        field.put("allowHalf", true);
        field.put("showText", true);
        return field;
    }
}