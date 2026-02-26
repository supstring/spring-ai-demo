package com.example.aianalysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 数据分析响应 DTO - 前端可直接消费的 JSON 格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAnalysisResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 昨天日期
     */
    private String yesterday;

    /**
     * 前天日期
     */
    private String dayBeforeYesterday;

    /**
     * 昨天原始数据摘要
     */
    private Map<String, Object> yesterdaySummary;

    /**
     * 前天原始数据摘要
     */
    private Map<String, Object> dayBeforeSummary;

    /**
     * 指标变化列表（如：订单量 +15.3%, 销售额 -2.1%）
     */
    private List<MetricChange> metricChanges;

    /**
     * AI 生成的文字分析报告
     */
    private String analysisReport;

    /**
     * 单个指标变化
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricChange {
        private String metricName;
        private Object yesterdayValue;
        private Object dayBeforeValue;
        private Double changePercent;
        private String trend;  // "up" | "down" | "unchanged"
    }
}
