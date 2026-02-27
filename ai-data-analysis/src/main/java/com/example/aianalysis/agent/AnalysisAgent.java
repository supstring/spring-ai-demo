package com.example.aianalysis.agent;

import com.example.aianalysis.mcp.McpSkillAdapter;
import com.example.aianalysis.skill.SkillChain;
import com.example.aianalysis.skill.SkillContext;
import com.example.aianalysis.skill.SkillExecutor;
import com.example.aianalysis.skill.impl.DataAnalysisSkill;
import com.example.aianalysis.skill.impl.ReportGenerationSkill;
import com.example.aianalysis.skill.impl.SqlGenerationSkill;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI 数据分析 Agent
 * 
 * 这是一个完整的 AI Agent 示例，展示如何：
 * 1. 使用 Skill 封装各种能力
 * 2. 通过 MCP 调用外部服务（ClickHouse）
 * 3. 编排多个 Skill 完成复杂任务
 * 
 * 工作流程：
 * 自然语言需求 → SQL生成Skill → MCP查询Skill → 数据分析Skill → 报告生成Skill → 最终报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisAgent {

    private final SkillExecutor skillExecutor;
    private final SqlGenerationSkill sqlGenerationSkill;
    private final DataAnalysisSkill dataAnalysisSkill;
    private final ReportGenerationSkill reportGenerationSkill;
    private final McpSkillAdapter mcpSkillAdapter;

    /**
     * 执行完整的数据分析任务
     * 
     * 示例：
     * "分析昨天用户活跃数据，对比前天的变化"
     * 
     * @param request 分析请求
     * @return 分析结果
     */
    public AnalysisResult analyze(AgentRequest request) {
        log.info("AI Agent 开始执行任务: {}", request.getUserQuery());

        // 创建执行上下文
        SkillContext context = SkillContext.builder()
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .build();

        try {
            // ========== Step 1: SQL 生成 ==========
            log.info("Step 1: 生成 SQL 查询");
            SqlGenerationSkill.Input sqlInput = SqlGenerationSkill.Input.builder()
                    .naturalLanguageQuery(request.getUserQuery())
                    .tableSchema(request.getTableSchema())
                    .databaseType("clickhouse")
                    .build();

            SqlGenerationSkill.Output sqlOutput = sqlGenerationSkill.execute(sqlInput);
            
            if (sqlOutput.getSql().isEmpty()) {
                return AnalysisResult.builder()
                        .success(false)
                        .error("SQL 生成失败: " + sqlOutput.getExplanation())
                        .build();
            }

            context.set("generatedSql", sqlOutput.getSql());
            log.info("生成的 SQL: {}", sqlOutput.getSql());

            // ========== Step 2: MCP 数据查询 ==========
            log.info("Step 2: 通过 MCP 查询数据");
            McpSkillAdapter.McpQueryInput queryInput = McpSkillAdapter.McpQueryInput.builder()
                    .sql(sqlOutput.getSql())
                    .timeoutMs(30000)
                    .build();

            McpSkillAdapter.McpQueryOutput queryOutput = mcpSkillAdapter
                    .createQuerySkill()
                    .execute(queryInput);

            if (!queryOutput.isSuccess()) {
                return AnalysisResult.builder()
                        .success(false)
                        .error("数据查询失败: " + queryOutput.getErrorMessage())
                        .build();
            }

            context.set("queryData", queryOutput.getData());
            log.info("查询完成，返回 {} 条数据", queryOutput.getData().size());

            // ========== Step 3: 数据分析 ==========
            log.info("Step 3: AI 数据分析");
            DataAnalysisSkill.Input analysisInput = DataAnalysisSkill.Input.builder()
                    .data(queryOutput.getData().toString())
                    .analysisType(request.getAnalysisType())
                    .context(request.getBusinessContext())
                    .build();

            DataAnalysisSkill.Output analysisOutput = dataAnalysisSkill.execute(analysisInput);
            context.set("analysisResult", analysisOutput);

            // ========== Step 4: 报告生成 ==========
            log.info("Step 4: 生成报告");
            ReportGenerationSkill.Input reportInput = ReportGenerationSkill.Input.builder()
                    .title(request.getReportTitle())
                    .period(request.getPeriod())
                    .summary("基于用户查询: " + request.getUserQuery())
                    .data(queryOutput.getData().toString())
                    .analysis(analysisOutput.getAnalysisReport())
                    .recommendations(String.join("\n", analysisOutput.getRecommendations()))
                    .format(request.getReportFormat())
                    .build();

            ReportGenerationSkill.Output reportOutput = reportGenerationSkill.execute(reportInput);

            // 构建最终结果
            return AnalysisResult.builder()
                    .success(true)
                    .sql(sqlOutput.getSql())
                    .data(queryOutput.getData())
                    .analysis(analysisOutput)
                    .report(reportOutput)
                    .executionContext(context)
                    .build();

        } catch (Exception e) {
            log.error("AI Agent 执行失败", e);
            return AnalysisResult.builder()
                    .success(false)
                    .error("执行失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 使用 Skill Chain 执行分析（更灵活的编排方式）
     */
    public AnalysisResult analyzeWithChain(AgentRequest request) {
        log.info("使用 Skill Chain 执行分析");

        SkillContext context = SkillContext.builder()
                .sessionId(request.getSessionId())
                .build();

        // 注册所有需要的 Skill
        skillExecutor.registerSkill(sqlGenerationSkill);
        skillExecutor.registerSkill(mcpSkillAdapter.createQuerySkill());
        skillExecutor.registerSkill(dataAnalysisSkill);
        skillExecutor.registerSkill(reportGenerationSkill);

        // 构建 Skill Chain
        SkillChain chain = SkillChain.builder()
                .name("data-analysis-chain")
                .description("完整的数据分析流程")
                .step(SkillChain.Step.builder()
                        .skillName("sql-generation")
                        .description("生成 SQL")
                        .outputTransformer((result, ctx) -> {
                            SqlGenerationSkill.Output output = (SqlGenerationSkill.Output) result;
                            ctx.set("sql", output.getSql());
                            return McpSkillAdapter.McpQueryInput.builder()
                                    .sql(output.getSql())
                                    .build();
                        })
                        .build())
                .step(SkillChain.Step.builder()
                        .skillName("mcp-clickhouse-query")
                        .description("查询数据")
                        .outputTransformer((result, ctx) -> {
                            McpSkillAdapter.McpQueryOutput output = (McpSkillAdapter.McpQueryOutput) result;
                            ctx.set("data", output.getData());
                            return DataAnalysisSkill.Input.builder()
                                    .data(output.getData().toString())
                                    .analysisType(request.getAnalysisType())
                                    .context(request.getBusinessContext())
                                    .build();
                        })
                        .build())
                .step(SkillChain.Step.builder()
                        .skillName("data-analysis")
                        .description("分析数据")
                        .build())
                .build();

        // 执行 Chain
        Object result = skillExecutor.executeChain(chain, 
                SqlGenerationSkill.Input.builder()
                        .naturalLanguageQuery(request.getUserQuery())
                        .tableSchema(request.getTableSchema())
                        .build(),
                context);

        log.info("Skill Chain 执行完成");
        
        return AnalysisResult.builder()
                .success(true)
                .executionContext(context)
                .build();
    }

    /**
     * 获取可用的 Skill 列表（供 AI 选择）
     */
    public List<SkillInfo> listAvailableSkills() {
        return List.of(
                SkillInfo.builder()
                        .name("sql-generation")
                        .description("根据自然语言生成 SQL")
                        .category("data")
                        .build(),
                SkillInfo.builder()
                        .name("mcp-clickhouse-query")
                        .description("通过 MCP 查询 ClickHouse")
                        .category("data")
                        .build(),
                SkillInfo.builder()
                        .name("data-analysis")
                        .description("AI 数据分析")
                        .category("analysis")
                        .build(),
                SkillInfo.builder()
                        .name("report-generation")
                        .description("生成分析报告")
                        .category("output")
                        .build()
        );
    }

    // ============ DTO 定义 ============

    @Data
    @Builder(toBuilder = true)
    public static class AgentRequest {
        private String userQuery;           // 用户的自然语言查询
        private String tableSchema;         // 表结构信息
        private String analysisType;        // 分析类型: trend, anomaly, comparison
        private String businessContext;     // 业务背景
        private String reportTitle;         // 报告标题
        private String period;              // 数据周期
        private String reportFormat;        // 报告格式: json, markdown, html
        private String sessionId;           // 会话 ID
        private String userId;              // 用户 ID
    }

    @Data
    @Builder
    public static class AnalysisResult {
        private boolean success;
        private String error;
        private String sql;
        private List<Map<String, Object>> data;
        private DataAnalysisSkill.Output analysis;
        private ReportGenerationSkill.Output report;
        private SkillContext executionContext;
    }

    @Data
    @Builder
    public static class SkillInfo {
        private String name;
        private String description;
        private String category;
    }
}
