package com.example.aianalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据分析请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataAnalysisRequest {

    /**
     * 可选：指定要分析的表名，若不指定则使用默认配置
     */
    private String tableName;

    /**
     * 可选：指定数据库名
     */
    private String databaseName;

    /**
     * 日期字段名，用于按日期筛选，默认 "date"
     */
    private String dateColumn = "date";

    /**
     * 可选：指标字段列表，用于聚合分析
     */
    private String[] metricColumns;

    /**
     * 可选：指定 AI 模型名或别名（如 deepseek、deepseek-chat、deepseek-reasoner）
     */
    private String modelName;

    /**
     * 可选：指定 AI 提供商（如 deepseek、openai、qwen）
     */
    private String provider;
}
