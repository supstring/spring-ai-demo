package com.example.aianalysis.controller;

import com.example.aianalysis.agent.AnalysisAgent;
import com.example.aianalysis.agent.AnalysisAgent.AgentRequest;
import com.example.aianalysis.agent.AnalysisAgent.AnalysisResult;
import com.example.aianalysis.agent.AnalysisAgent.SkillInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * AI Agent REST API 控制器
 * 
 * 提供以下接口：
 * 1. POST /api/agent/analyze - 执行数据分析
 * 2. GET /api/agent/skills - 获取可用 Skill 列表
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AnalysisAgent analysisAgent;

    /**
     * 执行 AI 数据分析
     * 
     * 请求示例：
     * {
     *   "userQuery": "查询昨天各渠道的用户注册量",
     *   "tableSchema": "表名: users, 字段: id, channel, created_at",
     *   "analysisType": "trend",
     *   "reportFormat": "markdown"
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
}
