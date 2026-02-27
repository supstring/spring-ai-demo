package com.example.aianalysis.skill.impl;

import com.example.aianalysis.skill.Skill;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SQL 生成 Skill
 * 
 * 使用 AI 将自然语言需求转换为 SQL 查询语句
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlGenerationSkill implements Skill<SqlGenerationSkill.Input, SqlGenerationSkill.Output> {

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public String getName() {
        return "sql-generation";
    }

    @Override
    public String getDescription() {
        return "根据自然语言描述生成 SQL 查询语句，支持 ClickHouse 方言";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "naturalLanguageQuery", "自然语言描述的数据查询需求",
            "tableSchema", "表结构信息",
            "databaseType", "数据库类型，如 clickhouse、mysql"
        );
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return Map.of(
            "sql", "生成的 SQL 语句",
            "explanation", "SQL 语句的解释说明",
            "confidence", "置信度 0-1"
        );
    }

    @Override
    public Output execute(Input input) {
        log.info("执行 SQL 生成 Skill，需求: {}", input.getNaturalLanguageQuery());

        String prompt = buildPrompt(input);

        try {
            String response = chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 解析 AI 响应，提取 SQL
            SqlParseResult result = parseSqlResponse(response);

            return Output.builder()
                    .sql(result.sql)
                    .explanation(result.explanation)
                    .confidence(result.confidence)
                    .rawResponse(response)
                    .build();

        } catch (Exception e) {
            log.error("SQL 生成失败", e);
            return Output.builder()
                    .sql("")
                    .explanation("生成失败: " + e.getMessage())
                    .confidence(0.0)
                    .build();
        }
    }

    private String buildPrompt(Input input) {
        return """
                你是一个专业的 SQL 工程师，擅长编写 ClickHouse SQL 查询。
                
                表结构信息：
                %s
                
                用户需求：
                %s
                
                请生成 SQL 查询，并按以下格式返回：
                
                SQL:
                ```sql
                [生成的 SQL 语句]
                ```
                
                解释:
                [SQL 语句的简要说明]
                
                置信度: [0-1 之间的数字]
                """.formatted(
                input.getTableSchema(),
                input.getNaturalLanguageQuery()
        );
    }

    private SqlParseResult parseSqlResponse(String response) {
        SqlParseResult result = new SqlParseResult();

        // 简单解析逻辑（实际项目中可以使用更健壮的解析方式）
        if (response.contains("```sql")) {
            int start = response.indexOf("```sql") + 6;
            int end = response.indexOf("```", start);
            if (end > start) {
                result.sql = response.substring(start, end).trim();
            }
        }

        if (response.contains("解释:")) {
            int start = response.indexOf("解释:") + 3;
            int end = response.indexOf("置信度:", start);
            if (end > start) {
                result.explanation = response.substring(start, end).trim();
            } else {
                result.explanation = response.substring(start).trim();
            }
        }

        if (response.contains("置信度:")) {
            String confidenceStr = response.substring(response.indexOf("置信度:") + 4).trim();
            try {
                result.confidence = Double.parseDouble(confidenceStr.split("\\s+")[0]);
            } catch (Exception e) {
                result.confidence = 0.8;
            }
        }

        return result;
    }

    private static class SqlParseResult {
        String sql = "";
        String explanation = "";
        double confidence = 0.8;
    }

    /**
     * 输入参数
     */
    @Data
    @Builder
    public static class Input {
        private String naturalLanguageQuery;
        private String tableSchema;
        private String databaseType;
    }

    /**
     * 输出结果
     */
    @Data
    @Builder
    public static class Output {
        private String sql;
        private String explanation;
        private double confidence;
        private String rawResponse;
    }
}
