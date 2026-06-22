package com.ccsurvey.modules.agent.protocol;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * ReAct 协议实现
 * ReAct = Reasoning + Acting
 * 实现 Thought -> Action -> Observation 循环
 */
@Data
public class ReActProtocol {

    /**
     * 思考结果
     */
    @Data
    public static class Thought {
        /**
         * 推理过程
         */
        private String reasoning;

        /**
         * 要执行的动作（工具名称）
         */
        private String action;

        /**
         * 动作参数
         */
        private Map<String, Object> actionInput;

        /**
         * 是否为最终答案
         */
        private boolean hasFinalAnswer;

        /**
         * 最终答案内容
         */
        private String finalAnswer;
    }

    /**
     * 观察结果
     */
    @Data
    public static class Observation {
        /**
         * 工具执行结果
         */
        private String result;

        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 错误信息
         */
        private String error;

        /**
         * 创建成功观察
         */
        public static Observation success(String result) {
            Observation obs = new Observation();
            obs.setResult(result);
            obs.setSuccess(true);
            return obs;
        }

        /**
         * 创建失败观察
         */
        public static Observation failure(String error) {
            Observation obs = new Observation();
            obs.setSuccess(false);
            obs.setError(error);
            return obs;
        }
    }

    /**
     * ReAct 步骤记录
     */
    @Data
    public static class Step {
        private int stepNumber;
        private Thought thought;
        private Observation observation;
    }

    /**
     * ReAct 执行结果
     */
    @Data
    public static class ReActResult {
        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 最终答案
         */
        private String finalAnswer;

        /**
         * 执行步骤
         */
        private List<Step> steps;

        /**
         * 总迭代次数
         */
        private int totalIterations;

        /**
         * 错误信息
         */
        private String error;
    }

    /**
     * 解析 LLM 输出为 Thought
     */
    public Thought parseThought(String llmOutput) {
        Thought thought = new Thought();

        // 检查是否为最终答案
        if (llmOutput.contains("Final Answer:") || llmOutput.contains("最终答案：")) {
            thought.setHasFinalAnswer(true);
            thought.setFinalAnswer(extractFinalAnswer(llmOutput));
            return thought;
        }

        // 解析 Action 和 Action Input
        thought.setAction(extractAction(llmOutput));
        thought.setActionInput(extractActionInput(llmOutput));
        thought.setReasoning(extractReasoning(llmOutput));
        thought.setHasFinalAnswer(false);

        return thought;
    }

    /**
     * 格式化观察结果
     */
    public String formatObservation(Observation observation) {
        if (observation.isSuccess()) {
            return "Observation: " + observation.getResult();
        } else {
            return "Observation: Error - " + observation.getError();
        }
    }

    /**
     * 构建 ReAct 提示词
     */
    public String buildPrompt(String systemPrompt, String userMessage, List<String> history) {
        StringBuilder sb = new StringBuilder();

        sb.append(systemPrompt);
        sb.append("\n\n");

        // 添加历史步骤
        if (history != null && !history.isEmpty()) {
            for (String step : history) {
                sb.append(step).append("\n");
            }
        }

        sb.append("\nQuestion: ").append(userMessage);
        sb.append("\n\nThought: ");

        return sb.toString();
    }

    /**
     * 提取最终答案
     */
    private String extractFinalAnswer(String output) {
        int idx = output.indexOf("Final Answer:");
        if (idx == -1) {
            idx = output.indexOf("最终答案：");
        }
        if (idx != -1) {
            return output.substring(idx).trim();
        }
        return output;
    }

    /**
     * 提取动作
     */
    private String extractAction(String output) {
        int start = output.indexOf("Action:");
        if (start == -1) {
            start = output.indexOf("动作：");
        }
        if (start != -1) {
            int end = output.indexOf("\n", start);
            if (end == -1) {
                end = output.indexOf("Action Input:", start);
            }
            if (end == -1) {
                end = output.indexOf("动作参数：", start);
            }
            if (end != -1) {
                return output.substring(start, end).replace("Action:", "").replace("动作：", "").trim();
            }
        }
        return null;
    }

    /**
     * 提取动作参数
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractActionInput(String output) {
        int start = output.indexOf("Action Input:");
        if (start == -1) {
            start = output.indexOf("动作参数：");
        }
        if (start != -1) {
            String jsonStr = output.substring(start)
                    .replace("Action Input:", "")
                    .replace("动作参数：", "")
                    .trim();

            // 尝试解析 JSON
            try {
                return com.alibaba.fastjson2.JSON.parseObject(jsonStr, Map.class);
            } catch (Exception e) {
                // 解析失败，返回空 Map
            }
        }
        return Map.of();
    }

    /**
     * 提取推理过程
     */
    private String extractReasoning(String output) {
        int start = output.indexOf("Thought:");
        if (start == -1) {
            start = output.indexOf("思考：");
        }
        if (start != -1) {
            int end = output.indexOf("\n", start);
            if (end == -1) {
                end = output.indexOf("Action:", start);
            }
            if (end == -1) {
                end = output.indexOf("动作：", start);
            }
            if (end != -1 && end > start) {
                return output.substring(start, end)
                        .replace("Thought:", "")
                        .replace("思考：", "")
                        .trim();
            }
        }
        return null;
    }
}