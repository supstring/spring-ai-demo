package com.example.aianalysis.skill.impl;

import com.example.aianalysis.skill.Skill;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 报告生成 Skill
 * 
 * 将分析结果格式化为结构化报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGenerationSkill implements Skill<ReportGenerationSkill.Input, ReportGenerationSkill.Output> {

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public String getName() {
        return "report-generation";
    }

    @Override
    public String getDescription() {
        return "将数据和分析结果生成格式化的业务报告";
    }

    @Override
    public Output execute(Input input) {
        log.info("执行报告生成 Skill，格式: {}", input.getFormat());

        return switch (input.getFormat().toLowerCase()) {
            case "json" -> generateJsonReport(input);
            case "markdown" -> generateMarkdownReport(input);
            case "html" -> generateHtmlReport(input);
            default -> generateTextReport(input);
        };
    }

    private Output generateJsonReport(Input input) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String jsonTemplate = """
                {
                  "reportMeta": {
                    "title": "%s",
                    "generatedAt": "%s",
                    "period": "%s",
                    "format": "json"
                  },
                  "summary": {
                    "description": "%s"
                  },
                  "data": %s,
                  "analysis": {
                    "findings": "%s"
                  },
                  "conclusion": {
                    "recommendations": "%s"
                  }
                }
                """;

        String json = String.format(jsonTemplate,
                escapeJson(input.getTitle()),
                timestamp,
                escapeJson(input.getPeriod()),
                escapeJson(input.getSummary()),
                input.getData(),
                escapeJson(input.getAnalysis()),
                escapeJson(input.getRecommendations())
        );

        return Output.builder()
                .content(json)
                .format("json")
                .build();
    }

    private Output generateMarkdownReport(Input input) {
        String markdown = """
                # %s
                
                > 生成时间: %s  
                > 数据周期: %s
                
                ## 执行摘要
                
                %s
                
                ## 数据概览
                
                ```json
                %s
                ```
                
                ## 详细分析
                
                %s
                
                ## 建议与行动项
                
                %s
                
                ---
                *本报告由 AI 数据分析系统自动生成*
                """.formatted(
                input.getTitle(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                input.getPeriod(),
                input.getSummary(),
                input.getData(),
                input.getAnalysis(),
                input.getRecommendations()
        );

        return Output.builder()
                .content(markdown)
                .format("markdown")
                .build();
    }

    private Output generateHtmlReport(Input input) {
        // 简化版 HTML 报告
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>%s</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .header { border-bottom: 2px solid #333; padding-bottom: 10px; }
                        .section { margin: 20px 0; }
                        .data-block { background: #f5f5f5; padding: 15px; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>%s</h1>
                        <p>周期: %s | 生成时间: %s</p>
                    </div>
                    <div class="section">
                        <h2>执行摘要</h2>
                        <p>%s</p>
                    </div>
                    <div class="section">
                        <h2>数据分析</h2>
                        <div class="data-block">%s</div>
                    </div>
                </body>
                </html>
                """.formatted(
                input.getTitle(),
                input.getTitle(),
                input.getPeriod(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                input.getSummary(),
                input.getAnalysis().replace("\n", "<br>")
        );

        return Output.builder()
                .content(html)
                .format("html")
                .build();
    }

    private Output generateTextReport(Input input) {
        String text = """
                ======================================
                %s
                ======================================
                周期: %s
                生成时间: %s
                
                【摘要】
                %s
                
                【数据】
                %s
                
                【分析】
                %s
                
                【建议】
                %s
                ======================================
                """.formatted(
                input.getTitle(),
                input.getPeriod(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                input.getSummary(),
                input.getData(),
                input.getAnalysis(),
                input.getRecommendations()
        );

        return Output.builder()
                .content(text)
                .format("text")
                .build();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 输入参数
     */
    @Data
    @Builder
    public static class Input {
        private String title;
        private String period;
        private String summary;
        private String data;
        private String analysis;
        private String recommendations;
        private String format; // json, markdown, html, text
    }

    /**
     * 输出结果
     */
    @Data
    @Builder
    public static class Output {
        private String content;
        private String format;
    }
}
