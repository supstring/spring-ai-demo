package com.example.aianalysis.mcp;

import com.example.aianalysis.skill.Skill;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Skill 适配器
 *
 * 将 MCP Server 的能力封装为 Skill，使 AI Agent 可以统一调用
 *
 * 设计模式：适配器模式 + 外观模式
 *
 * 注意：这是一个简化实现，实际 MCP 调用需要根据具体的 MCP Server 实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpSkillAdapter {

    private final McpDataQueryService mcpDataQueryService;

    /**
     * 创建数据查询 Skill（基于 MCP）
     */
    public Skill<McpQueryInput, McpQueryOutput> createQuerySkill() {
        return new McpQuerySkill();
    }

    /**
     * 创建 Schema 探索 Skill（基于 MCP）
     */
    public Skill<SchemaExploreInput, SchemaExploreOutput> createSchemaExploreSkill() {
        return new McpSchemaExploreSkill();
    }

    /**
     * MCP 数据查询 Skill 实现
     */
    public class McpQuerySkill implements Skill<McpQueryInput, McpQueryOutput> {

        @Override
        public String getName() {
            return "mcp-clickhouse-query";
        }

        @Override
        public String getDescription() {
            return "通过 MCP 协议查询 ClickHouse 数据库，支持执行任意 SELECT 语句";
        }

        @Override
        public Map<String, Object> getInputSchema() {
            return Map.of(
                "sql", "要执行的 SQL 查询语句",
                "timeoutMs", "超时时间（毫秒），默认 30000"
            );
        }

        @Override
        public McpQueryOutput execute(McpQueryInput input) {
            log.info("执行 MCP 查询 Skill: {}", input.getSql());

            try {
                long startTime = System.currentTimeMillis();
                List<Map<String, Object>> data = mcpDataQueryService.executeQuery(input.getSql());
                long duration = System.currentTimeMillis() - startTime;

                return McpQueryOutput.builder()
                        .success(true)
                        .data(data)
                        .executionTimeMs(duration)
                        .build();

            } catch (Exception e) {
                log.error("MCP 查询执行失败", e);
                return McpQueryOutput.builder()
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
            }
        }
    }

    /**
     * MCP Schema 探索 Skill 实现
     */
    public class McpSchemaExploreSkill implements Skill<SchemaExploreInput, SchemaExploreOutput> {

        @Override
        public String getName() {
            return "mcp-schema-explore";
        }

        @Override
        public String getDescription() {
            return "探索 ClickHouse 数据库的表结构和字段信息";
        }

        @Override
        public SchemaExploreOutput execute(SchemaExploreInput input) {
            log.info("执行 Schema 探索 Skill，数据库: {}", input.getDatabase());

            try {
                // 查询表列表
                String tablesSql = String.format(
                    "SELECT name, engine, total_rows, total_bytes " +
                    "FROM system.tables WHERE database = '%s'",
                    input.getDatabase()
                );

                List<Map<String, Object>> result = mcpDataQueryService.executeQuery(tablesSql);

                return SchemaExploreOutput.builder()
                        .success(true)
                        .database(input.getDatabase())
                        .tables(parseTables(result))
                        .build();

            } catch (Exception e) {
                log.error("Schema 探索失败", e);
                return SchemaExploreOutput.builder()
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
            }
        }

        private List<TableInfo> parseTables(List<Map<String, Object>> result) {
            // 简化实现
            return List.of();
        }
    }

    // ============ 输入输出 DTO ============

    @Data
    @Builder
    public static class McpQueryInput {
        private String sql;
        private int timeoutMs;
    }

    @Data
    @Builder
    public static class McpQueryOutput {
        private boolean success;
        private List<Map<String, Object>> data;
        private long executionTimeMs;
        private String errorMessage;

        public int getRowCount() {
            return data != null ? data.size() : 0;
        }
    }

    @Data
    @Builder
    public static class SchemaExploreInput {
        private String database;
        private String tablePattern; // 可选，过滤表名
    }

    @Data
    @Builder
    public static class SchemaExploreOutput {
        private boolean success;
        private String database;
        private List<TableInfo> tables;
        private String errorMessage;
    }

    @Data
    @Builder
    public static class TableInfo {
        private String name;
        private String engine;
        private long rowCount;
        private long sizeBytes;
        private List<ColumnInfo> columns;
    }

    @Data
    @Builder
    public static class ColumnInfo {
        private String name;
        private String type;
        private String comment;
    }
}
