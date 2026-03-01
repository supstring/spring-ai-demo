package com.example.aianalysis.controller;

import com.example.aianalysis.dto.DataAnalysisRequest;
import com.example.aianalysis.dto.DataAnalysisResponse;
import com.example.aianalysis.service.DataAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 数据分析 REST API - 供前端调用
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DataAnalysisController {

    private final DataAnalysisService dataAnalysisService;

    /**
     * 查询昨天与前天的数据变化分析
     *
     * @param request 可选的分析参数
     * @return JSON 格式的分析结果，前端可直接消费
     */
    @PostMapping(value = "/data-analysis", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataAnalysisResponse> analyzeDataChange(
            @RequestBody(required = false) @Valid DataAnalysisRequest request) {
        DataAnalysisRequest req = request != null ? request : new DataAnalysisRequest();
        DataAnalysisResponse response = dataAnalysisService.analyzeYesterdayVsDayBefore(req);
        return ResponseEntity.ok(response);
    }

    /**
     * GET 简化接口 - 使用默认配置进行分析
     */
    @GetMapping(value = "/data-analysis", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataAnalysisResponse> analyzeDataChangeDefault() {
        DataAnalysisResponse response = dataAnalysisService.analyzeYesterdayVsDayBefore(
                DataAnalysisRequest.builder().dateColumn("date").build());
        return ResponseEntity.ok(response);
    }


}
