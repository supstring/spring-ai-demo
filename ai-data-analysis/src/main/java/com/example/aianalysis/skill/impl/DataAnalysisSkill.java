package com.example.aianalysis.skill.impl;

import com.example.aianalysis.skill.Skill;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据分析 Skill
 * 
 * 使用 AI 分析数据并生成洞察报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataAnalysisSkill implements Skill<DataAnalysisSkill.Input, DataAnalysisSkill.Output> {

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public String getName() {
        return "data-analysis";
    }

    @Override
    public String getDescription() {
        return "分析数据趋势、异常和关键指标，生成业务洞察报告";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "data", "要分析的数据，可以是 JSON 或表格格式",
            "analysisType", "分析类型：trend(趋势)、anomaly(异常检测)、comparison(对比分析)",
            "context", "业务背景信息"
        );
    }

    @Override
    public Output execute(Input input) {
        log.info("执行数据分析 Skill，类型: {}", input.getAnalysisType());

        String prompt = buildAnalysisPrompt(input);

        try {
            String analysis = chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 提取关键洞察点
            List<String> insights = extractInsights(analysis);

            return Output.builder()
                    .analysisReport(analysis)
                    .insights(insights)
                    .recommendations(extractRecommendations(analysis))
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("数据分析失败", e);
            return Output.builder()
                    .analysisReport("分析失败: " + e.getMessage())
                    .success(false)
                    .build();
        }
    }

    private String buildAnalysisPrompt(Input input) {
        String analysisTypeDesc = switch (input.getAnalysisType()) {
            case "trend" -> "分析数据趋势，识别增长/下降模式";
            case "anomaly" -> "检测数据中的异常点和异常模式";
            case "comparison" -> "对比不同时间段或维度的数据差异";
            default -> "进行全面数据分析";
        };

        return """
                作为数据分析师，请对以下数据进行深入分析。
                
                分析任务：%s
                
                业务背景：
                %s
                
                数据：
                %s
                
                请提供：
                1. 数据概览（关键指标总结）
                2. 主要发现（3-5 个关键洞察）
                3. 异常识别（如有）
                4. 趋势分析
                5. 行动建议（2-3 条具体建议）
                
                请用中文回复，保持简洁专业。
                """.formatted(
                analysisTypeDesc,
                input.getContext(),
                input.getData()
        );
    }

    private List<String> extractInsights(String analysis) {
        // 简单提取以 "-" 或数字开头的行作为洞察点
        return analysis.lines()
                .filter(line -> line.trim().startsWith("-") || 
                        line.trim().matches("^\\d+\\.") ||
                        line.contains("发现") ||
                        line.contains("关键"))
                .map(String::trim)
                .limit(5)
                .toList();
    }

    private List<String> extractRecommendations(String analysis) {
        return analysis.lines()
                .filter(line -> line.contains("建议") || 
                        line.contains("推荐") ||
                        line.contains("行动"))
                .map(String::trim)
                .limit(3)
                .toList();
    }

    /**
     * 输入参数
     */
    @Data
    @Builder
    public static class Input {
        private String data;
        private String analysisType;
        private String context;
    }

    /**
     * 输出结果
     */
    @Data
    @Builder
    public static class Output {
        private String analysisReport;
        private List<String> insights;
        private List<String> recommendations;
        private boolean success;
    }
}
