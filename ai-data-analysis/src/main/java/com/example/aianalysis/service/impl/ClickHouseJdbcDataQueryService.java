package com.example.aianalysis.service.impl;

import com.example.aianalysis.dto.DataAnalysisRequest;
import com.example.aianalysis.service.DataQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过 ClickHouse JDBC 直接查询数据
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mcp.enabled", havingValue = "false", matchIfMissing = true)
public class ClickHouseJdbcDataQueryService implements DataQueryService {

    private final DataSource clickhouseDataSource;

    public ClickHouseJdbcDataQueryService(
            @org.springframework.beans.factory.annotation.Qualifier("clickhouseDataSource") DataSource clickhouseDataSource) {
        this.clickhouseDataSource = clickhouseDataSource;
    }

    @Override
    public Map<String, Object> queryDataSummary(String date, DataAnalysisRequest request) {
        String table = request.getTableName() != null ? request.getTableName() : "your_analytics_table";
        String db = request.getDatabaseName() != null ? request.getDatabaseName() : "default";
        String dateCol = request.getDateColumn() != null ? request.getDateColumn() : "date";

        // 示例：按日期聚合，统计 count 和 sum 等
        // 实际 SQL 需根据业务表结构调整
        String sql = String.format(
                "SELECT count() as cnt, sum(1) as total FROM %s.%s WHERE toDate(%s) = '%s'",
                db, table, dateCol, date
        );

        try {
            JdbcTemplate jdbc = new JdbcTemplate(clickhouseDataSource);
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            if (rows.isEmpty()) {
                return new HashMap<>(Map.of("count", 0L));
            }
            return rows.get(0);
        } catch (Exception e) {
            log.warn("ClickHouse 查询失败，返回模拟数据: {}", e.getMessage());
            // 开发阶段可返回模拟数据
            return Map.of("count", 0L, "total", 0L);
        }
    }
}
