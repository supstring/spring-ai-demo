package com.example.aianalysis.controller;

import com.example.aianalysis.agent.AnalysisAgent;
import com.example.aianalysis.agent.AnalysisAgent.AgentRequest;
import com.example.aianalysis.agent.AnalysisAgent.AnalysisResult;
import com.example.aianalysis.agent.AnalysisAgent.SkillInfo;
import com.example.aianalysis.mcp.McdMcpService;
import com.example.aianalysis.mcp.McpDataQueryService;
import com.example.aianalysis.mcp.McpSkillAdapter;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI Agent REST API 控制器
 * 
 * 提供以下接口：
 * 1. POST /api/agent/analyze - 执行数据分析
 * 2. GET /api/agent/skills - 获取可用 Skill 列表
 * 3. POST /api/agent/test/mcp - 测试 MCP 调用
 * 4. POST /api/agent/test/mcd/* - 麦当劳 MCP 测试
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AnalysisAgent analysisAgent;
    private final McpDataQueryService mcpDataQueryService;
    private final McpSkillAdapter mcpSkillAdapter;
    private final McdMcpService mcdMcpService;

    /**
     * 执行 AI 数据分析
     * 
     * 请求示例：
     * {
     *   "userQuery": "查询昨天各渠道的用户注册量",
     *   "tableSchema": "表名: users, 字段: id, channel, created_at",
     *   "analysisType": "trend",
     *   "reportFormat": "markdown",
     *   "provider": "deepseek",
     *   "modelName": "deepseek"
     * }
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyze(@RequestBody AgentRequest request) {
        log.info("收到分析请求: {}", request.getUserQuery());

        // 设置默认会话 ID
        if (request.getSessionId() == null) {
            request = request.toBuilder()
                    .sessionId(UUID.randomUUID().toString())
                    .build();
        }

        AnalysisResult result = analysisAgent.analyze(request);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 使用 Skill Chain 执行分析
     */
    @PostMapping("/analyze/chain")
    public ResponseEntity<AnalysisResult> analyzeWithChain(@RequestBody AgentRequest request) {
        log.info("收到 Chain 分析请求: {}", request.getUserQuery());

        if (request.getSessionId() == null) {
            request = request.toBuilder()
                    .sessionId(UUID.randomUUID().toString())
                    .build();
        }

        AnalysisResult result = analysisAgent.analyzeWithChain(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取可用的 Skill 列表
     */
    @GetMapping("/skills")
    public ResponseEntity<List<SkillInfo>> listSkills() {
        return ResponseEntity.ok(analysisAgent.listAvailableSkills());
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Agent is running");
    }

    // ============ MCP 测试接口 ============

    /**
     * 测试 MCP 数据查询
     * 
     * 请求示例：
     * {
     *   "sql": "SELECT 1 as test"
     * }
     */
    @PostMapping("/test/mcp/query")
    public ResponseEntity<McpTestResult> testMcpQuery(@RequestBody McpTestRequest request) {
        log.info("收到 MCP 测试请求: {}", request.getSql());

        try {
            // 使用 McpSkillAdapter 执行查询
            McpSkillAdapter.McpQueryInput input = McpSkillAdapter.McpQueryInput.builder()
                    .sql(request.getSql())
                    .timeoutMs(request.getTimeoutMs() != null ? request.getTimeoutMs() : 30000)
                    .build();

            McpSkillAdapter.McpQueryOutput output = mcpSkillAdapter.createQuerySkill().execute(input);

            McpTestResult result = McpTestResult.builder()
                    .success(output.isSuccess())
                    .data(output.getData())
                    .executionTimeMs(output.getExecutionTimeMs())
                    .errorMessage(output.getErrorMessage())
                    .build();

            if (output.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("MCP 测试失败", e);
            return ResponseEntity.badRequest().body(
                    McpTestResult.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 测试 MCP Schema 探索
     * 
     * 请求示例：
     * {
     *   "database": "default"
     * }
     */
    @PostMapping("/test/mcp/schema")
    public ResponseEntity<McpSchemaResult> testMcpSchema(@RequestBody McpSchemaRequest request) {
        log.info("收到 MCP Schema 测试请求: {}", request.getDatabase());

        try {
            McpSkillAdapter.SchemaExploreInput input = McpSkillAdapter.SchemaExploreInput.builder()
                    .database(request.getDatabase())
                    .tablePattern(request.getTablePattern())
                    .build();

            McpSkillAdapter.SchemaExploreOutput output = mcpSkillAdapter.createSchemaExploreSkill().execute(input);

            McpSchemaResult result = McpSchemaResult.builder()
                    .success(output.isSuccess())
                    .database(output.getDatabase())
                    .tables(output.getTables())
                    .errorMessage(output.getErrorMessage())
                    .build();

            if (output.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("MCP Schema 测试失败", e);
            return ResponseEntity.badRequest().body(
                    McpSchemaResult.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 获取 MCP 工具列表
     */
    @GetMapping("/test/mcp/tools")
    public ResponseEntity<List<String>> listMcpTools() {
        log.info("获取 MCP 工具列表");
        return ResponseEntity.ok(mcpDataQueryService.listAvailableTools());
    }

    // ============ MCP 测试 DTO ============

    @Data
    @Builder
    public static class McpTestRequest {
        private String sql;
        private Integer timeoutMs;
    }

    @Data
    @Builder
    public static class McpTestResult {
        private boolean success;
        private List<Map<String, Object>> data;
        private long executionTimeMs;
        private String errorMessage;
    }

    @Data
    @Builder
    public static class McpSchemaRequest {
        private String database;
        private String tablePattern;
    }

    @Data
    @Builder
    public static class McpSchemaResult {
        private boolean success;
        private String database;
        private List<McpSkillAdapter.TableInfo> tables;
        private String errorMessage;
    }

    // ============ 麦当劳 MCP 测试接口 ============

    /**
     * 获取麦当劳服务器时间
     */
    @GetMapping("/test/mcd/time")
    public ResponseEntity<?> getMcdServerTime() {
        log.info("获取麦当劳服务器时间");
        return ResponseEntity.ok(mcdMcpService.getServerTime());
    }
}
