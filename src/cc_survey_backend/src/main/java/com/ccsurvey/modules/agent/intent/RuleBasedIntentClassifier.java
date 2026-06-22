package com.ccsurvey.modules.agent.intent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 规则匹配意图分类器
 * 通过关键词和正则表达式快速识别意图
 */
@Slf4j
@Component
public class RuleBasedIntentClassifier implements IntentClassifier {

    /**
     * 意图规则配置
     */
    private final Map<Intent, IntentRule> rules;

    public RuleBasedIntentClassifier() {
        this.rules = initRules();
    }

    @Override
    public IntentResult classify(String message, Object context) {
        if (message == null || message.trim().isEmpty()) {
            return IntentResult.unknown();
        }

        String normalizedMessage = message.toLowerCase().trim();

        // 检查是否为多意图
        List<Intent> detectedIntents = new ArrayList<>();
        Map<String, Object> entities = new HashMap<>();

        for (Map.Entry<Intent, IntentRule> entry : rules.entrySet()) {
            Intent intent = entry.getKey();
            IntentRule rule = entry.getValue();

            if (matchesRule(normalizedMessage, rule)) {
                detectedIntents.add(intent);
                extractEntities(normalizedMessage, rule, entities);
            }
        }

        // 特殊处理：检查是否有待确认的预览
        // 如果上下文中有 pendingPreviews，且用户说"确认"/"保存"/"存草稿"等，优先返回 SAVE_DRAFT
        if (context instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> contextMap = (Map<String, Object>) context;
            Object pendingPreviews = contextMap.get("pendingPreviews");

            if (pendingPreviews instanceof Map && !((Map<?, ?>) pendingPreviews).isEmpty()) {
                // 有待确认的预览
                if (normalizedMessage.contains("确认") || normalizedMessage.contains("保存") ||
                    normalizedMessage.contains("存草稿") || normalizedMessage.contains("添加草稿") ||
                    normalizedMessage.equals("确认") || normalizedMessage.equals("保存") ||
                    normalizedMessage.equals("存") || normalizedMessage.equals("好") ||
                    normalizedMessage.equals("是的") || normalizedMessage.equals("对")) {

                    log.debug("Detected confirmation with pending preview, returning SAVE_DRAFT");
                    return IntentResult.simple(Intent.SAVE_DRAFT, 0.95f);
                }
            }
        }

        // 如果检测到多个意图，返回多意图结果
        if (detectedIntents.size() > 1) {
            log.debug("Detected multiple intents: {}", detectedIntents);
            return IntentResult.multi(detectedIntents, 0.9f);
        }

        // 如果检测到单个意图
        if (detectedIntents.size() == 1) {
            Intent intent = detectedIntents.get(0);
            log.debug("Detected single intent: {}", intent);
            return IntentResult.withEntities(intent, 0.9f, entities);
        }

        // 未检测到意图，返回通用对话
        log.debug("No specific intent detected, returning GENERAL_CHAT");
        return IntentResult.simple(Intent.GENERAL_CHAT, 0.5f);
    }

    @Override
    public String getName() {
        return "rule-based";
    }

    @Override
    public int getPriority() {
        return 10;  // 最高优先级，快速匹配
    }

    /**
     * 检查消息是否匹配规则
     */
    private boolean matchesRule(String message, IntentRule rule) {
        // 检查关键词
        for (String keyword : rule.getKeywords()) {
            if (message.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        // 检查正则表达式
        for (Pattern pattern : rule.getPatterns()) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 提取实体信息
     */
    private void extractEntities(String message, IntentRule rule, Map<String, Object> entities) {
        // 简单实体提取逻辑
        // 可以根据需要扩展更复杂的实体提取
    }

    /**
     * 初始化意图规则
     */
    private Map<Intent, IntentRule> initRules() {
        Map<Intent, IntentRule> rules = new LinkedHashMap<>();

        // 创建问卷规则 - 需要同时包含动作词和对象词
        rules.put(Intent.CREATE_SURVEY, IntentRule.builder()
                .keywords(Arrays.asList(
                        // 动作+对象组合词
                        "创建问卷", "新建问卷", "生成问卷", "制作问卷", "设计问卷",
                        "创建调查", "新建调查", "生成调查", "制作调查", "设计调查",
                        "创建表单", "新建表单", "生成表单", "制作表单", "设计表单",
                        "create survey", "build survey", "make survey", "design survey",
                        "create questionnaire", "build questionnaire"
                ))
                .patterns(Arrays.asList(
                        Pattern.compile(".*创建.*问卷.*"),
                        Pattern.compile(".*新建.*问卷.*"),
                        Pattern.compile(".*生成.*问卷.*"),
                        Pattern.compile(".*设计.*问卷.*"),
                        Pattern.compile(".*创建.*调查.*"),
                        Pattern.compile(".*新建.*调查.*"),
                        Pattern.compile(".*创建.*表单.*"),
                        Pattern.compile(".*survey.*create.*")
                ))
                .build());

        // 更新问卷规则
        rules.put(Intent.UPDATE_SURVEY, IntentRule.builder()
                .keywords(Arrays.asList(
                        "修改问卷", "更新问卷", "编辑问卷", "调整问卷", "变更问卷",
                        "修改调查", "更新调查", "编辑调查",
                        "修改表单", "更新表单", "编辑表单",
                        "update survey", "edit survey", "modify survey"
                ))
                .patterns(Arrays.asList(
                        Pattern.compile(".*修改.*问卷.*"),
                        Pattern.compile(".*更新.*问卷.*"),
                        Pattern.compile(".*编辑.*问卷.*"),
                        Pattern.compile(".*修改.*调查.*"),
                        Pattern.compile(".*编辑.*表单.*")
                ))
                .build());

        // 数据分析规则 - 优先级较高，覆盖所有数据统计相关查询
        rules.put(Intent.ANALYZE_DATA, IntentRule.builder()
                .keywords(Arrays.asList(
                        // 统计相关
                        "问卷总数", "提交总数", "统计总览", "数据总览", "总览",
                        "问卷数量", "提交数量", "有多少问卷", "有多少提交",
                        "分析数据", "数据分析", "统计分析", "查看数据", "数据统计",
                        "提交数据", "问卷数据", "答案分析", "统计报告",
                        "所有问卷", "问卷列表", "提交情况", "问卷情况",
                        // 英文关键词
                        "analyze data", "data analysis", "statistics", "submission data",
                        "total surveys", "total submissions", "survey count"
                ))
                .patterns(Arrays.asList(
                        // 总数/数量相关
                        Pattern.compile(".*问卷.*总数.*"),
                        Pattern.compile(".*提交.*总数.*"),
                        Pattern.compile(".*问卷.*数量.*"),
                        Pattern.compile(".*提交.*数量.*"),
                        Pattern.compile(".*有多少.*问卷.*"),
                        Pattern.compile(".*有多少.*提交.*"),
                        Pattern.compile(".*问卷.*统计.*"),
                        Pattern.compile(".*统计.*总览.*"),
                        Pattern.compile(".*数据.*总览.*"),
                        // 分析相关
                        Pattern.compile(".*分析.*数据.*"),
                        Pattern.compile(".*数据.*分析.*"),
                        Pattern.compile(".*统计.*数据.*"),
                        Pattern.compile(".*数据.*统计.*"),
                        Pattern.compile(".*查看.*数据.*"),
                        Pattern.compile(".*问卷.*提交.*"),
                        Pattern.compile(".*提交.*统计.*"),
                        // 列表相关
                        Pattern.compile(".*所有.*问卷.*"),
                        Pattern.compile(".*问卷.*列表.*"),
                        Pattern.compile(".*提交.*情况.*")
                ))
                .build());

        // 日志查询规则
        rules.put(Intent.QUERY_LOGS, IntentRule.builder()
                .keywords(Arrays.asList(
                        // 日志相关关键词（需要更具体的词）
                        "操作日志", "日志查询", "查看日志", "日志记录", "操作记录",
                        "活动日志", "审计日志", "系统日志", "日志列表", "日志分析",
                        "登录日志", "登录记录", "登录情况", "登录次数",
                        "提交日志", "提交记录", "提交情况", "提交次数",
                        // 动作+日志组合
                        "返回日志", "获取日志", "查询日志", "分析日志", "统计日志",
                        // 英文关键词
                        "operation log", "activity log", "audit log", "system log",
                        "login log", "login record", "login history"
                ))
                .patterns(Arrays.asList(
                        // 包含"日志"且有动词或限定词
                        Pattern.compile(".*日志.*"),
                        Pattern.compile(".*log.*"),
                        // 操作记录相关
                        Pattern.compile(".*操作.*记录.*"),
                        // 登录相关
                        Pattern.compile(".*登录.*系统.*"),
                        Pattern.compile(".*登录.*情况.*"),
                        Pattern.compile(".*登录.*次数.*"),
                        Pattern.compile(".*几次.*登录.*"),
                        // 提交相关
                        Pattern.compile(".*提交.*问卷.*"),
                        Pattern.compile(".*提交.*情况.*"),
                        Pattern.compile(".*提交.*次数.*"),
                        Pattern.compile(".*几次.*提交.*")
                ))
                .build());

        // 自然语言查询规则 - 包含时间词或模糊词的查询
        rules.put(Intent.NATURAL_QUERY, IntentRule.builder()
                .keywords(Arrays.asList(
                        // 时间相关查询词
                        "上周", "本周", "上月", "本月", "去年", "今年", "昨天", "前天",
                        "几天前", "几周前", "几个月前",
                        // 模糊查询词
                        "大概", "可能", "也许", "大概有", "可能有", "也许有",
                        "什么的", "之类的", "类似",
                        // 排行/最高/最低
                        "最高", "最低", "最多", "最少", "排行", "排名", "前几",
                        // 平均/统计
                        "平均分", "平均", "得分", "分数",
                        // 用户维度查询
                        "提交了几份", "提交多少", "提交了几个", "提交了几次",
                        "谁提交", "哪些人提交", "谁填了", "哪些人填",
                        // 无提交
                        "没人提交", "没有提交", "无人提交", "零提交",
                        // 英文
                        "last week", "this week", "last month", "this month",
                        "yesterday", "average", "top", "highest", "lowest"
                ))
                .patterns(Arrays.asList(
                        // 时间+统计组合
                        Pattern.compile(".*上.*周.*提交.*"),
                        Pattern.compile(".*上.*月.*提交.*"),
                        Pattern.compile(".*本.*周.*提交.*"),
                        Pattern.compile(".*本.*月.*提交.*"),
                        Pattern.compile(".*昨天.*提交.*"),
                        Pattern.compile(".*今天.*提交.*"),
                        // 模糊匹配
                        Pattern.compile(".*可能.*问卷.*"),
                        Pattern.compile(".*也许.*问卷.*"),
                        Pattern.compile(".*大概.*问卷.*"),
                        Pattern.compile(".*什么的.*问卷.*"),
                        Pattern.compile(".*之类.*问卷.*"),
                        // 排行
                        Pattern.compile(".*最高.*问卷.*"),
                        Pattern.compile(".*最多.*提交.*"),
                        Pattern.compile(".*排行.*问卷.*"),
                        Pattern.compile(".*排名.*问卷.*"),
                        Pattern.compile(".*前.*个.*问卷.*"),
                        // 平均分
                        Pattern.compile(".*平均分.*"),
                        Pattern.compile(".*平均.*分数.*"),
                        // 用户维度
                        Pattern.compile(".*提交.*几份.*"),
                        Pattern.compile(".*提交.*几个.*"),
                        Pattern.compile(".*提交.*几次.*"),
                        Pattern.compile(".*谁.*提交.*"),
                        Pattern.compile(".*哪些人.*提交.*"),
                        // 无提交
                        Pattern.compile(".*没.*提交.*问卷.*"),
                        Pattern.compile(".*没有.*提交.*问卷.*"),
                        Pattern.compile(".*无人.*提交.*"),
                        // 登录统计（带时间）
                        Pattern.compile(".*上.*周.*登录.*"),
                        Pattern.compile(".*上.*月.*登录.*"),
                        Pattern.compile(".*今天.*登录.*"),
                        Pattern.compile(".*昨天.*登录.*"),
                        Pattern.compile(".*几天.*登录.*"),
                        // 注册统计
                        Pattern.compile(".*注册.*用户.*"),
                        Pattern.compile(".*新用户.*"),
                        Pattern.compile(".*注册.*数量.*")
                ))
                .build());

        // 保存草稿规则
        rules.put(Intent.SAVE_DRAFT, IntentRule.builder()
                .keywords(Arrays.asList(
                        "存草稿", "保存草稿", "确认保存", "保存问卷", "存为草稿",
                        "添加草稿", "添加为草稿", "确认", "保存", "存",
                        "save draft", "confirm save", "save survey", "confirm"
                ))
                .patterns(Arrays.asList(
                        Pattern.compile(".*存.*草稿.*"),
                        Pattern.compile(".*保存.*草稿.*"),
                        Pattern.compile(".*确认.*保存.*"),
                        Pattern.compile(".*存为.*草稿.*"),
                        Pattern.compile(".*添加.*草稿.*"),
                        Pattern.compile(".*添加为.*草稿.*"),
                        Pattern.compile("^确认$"),
                        Pattern.compile("^保存$"),
                        Pattern.compile("^存$")
                ))
                .build());

        return rules;
    }

    /**
     * 意图规则定义
     */
    @lombok.Data
    @lombok.Builder
    private static class IntentRule {
        private List<String> keywords;
        private List<Pattern> patterns;
    }
}