package com.example.aianalysis.service;

import com.example.aianalysis.dto.DataAnalysisRequest;

import java.util.Map;

/**
 * 数据查询服务接口
 * 支持两种实现：1) 直接 ClickHouse JDBC  2) 通过 MCP 调用 ClickHouse MCP Server
 */
public interface DataQueryService {

    /**
     * 查询指定日期的数据摘要
     *
     * @param date        日期，格式 yyyy-MM-dd
     * @param request     分析请求参数
     * @return 该日期的聚合数据摘要，key 为指标名，value 为数值
     */
    Map<String, Object> queryDataSummary(String date, DataAnalysisRequest request);
}
