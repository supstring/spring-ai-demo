# AI 数据分析服务

基于 Java + Spring Boot 的 AI 数据分析项目，提供 REST 接口供前端调用。支持查询昨天与前天的数据，并通过 AI 生成变化分析报告。

## 技术栈

| 组件 | 框架/库 | 用途 |
|------|---------|------|
| Web 框架 | Spring Boot 3.4 | REST API、依赖注入 |
| AI 能力 | Spring AI (OpenAI) | 数据分析和自然语言报告生成 |
| 数据查询 | ClickHouse JDBC / MCP | 查询 ClickHouse 数据 |
| MCP 客户端 | MCP Java SDK | 调用 ClickHouse MCP Server |
| 数据源 | ClickHouse | 存储分析数据 |

## 项目结构

```
ai-data-analysis/
├── src/main/java/com/example/aianalysis/
│   ├── AiDataAnalysisApplication.java    # 启动类
│   ├── config/
│   │   └── ClickHouseConfig.java         # ClickHouse 数据源配置
│   ├── controller/
│   │   └── DataAnalysisController.java   # REST API
│   ├── dto/
│   │   ├── DataAnalysisRequest.java      # 请求 DTO
│   │   └── DataAnalysisResponse.java     # 响应 DTO (JSON)
│   └── service/
│       ├── DataAnalysisService.java      # 编排：查询 + AI 分析
│       ├── DataQueryService.java         # 数据查询接口
│       └── impl/
│           ├── ClickHouseJdbcDataQueryService.java  # JDBC 直连实现
│           └── McpDataQueryService.java             # MCP 调用实现
├── src/main/resources/
│   ├── application.yml
│   └── application-dev.yml
└── pom.xml
```

## 快速开始

### 1. 环境要求

- JDK 21+
- Maven 3.9+
- ClickHouse（可选，用于真实数据）
- OpenAI API Key（用于 AI 分析）

### 2. 配置

在 `application.yml` 或环境变量中配置：

```yaml
# OpenAI（必填，用于 AI 分析）
spring.ai.openai.api-key: ${OPENAI_API_KEY}

# ClickHouse（使用 JDBC 模式时）
clickhouse:
  url: jdbc:clickhouse://localhost:8123/default
  username: default
  password: ""

# 是否使用 MCP 模式（默认 false，使用 JDBC）
app.mcp.enabled: false
```

### 3. 运行

```bash
cd ai-data-analysis
mvn spring-boot:run
```

### 4. 调用接口

**GET 请求（默认分析）：**
```bash
curl http://localhost:8080/api/v1/data-analysis
```

**POST 请求（带参数）：**
```bash
curl -X POST http://localhost:8080/api/v1/data-analysis \
  -H "Content-Type: application/json" \
  -d '{"tableName":"your_table","dateColumn":"date"}'
```

**响应示例（JSON）：**
```json
{
  "success": true,
  "yesterday": "2025-01-14",
  "dayBeforeYesterday": "2025-01-13",
  "yesterdaySummary": {"cnt": 1200, "total": 1200},
  "dayBeforeSummary": {"cnt": 1100, "total": 1100},
  "metricChanges": [
    {
      "metricName": "cnt",
      "yesterdayValue": 1200,
      "dayBeforeValue": 1100,
      "changePercent": 9.1,
      "trend": "up"
    }
  ],
  "analysisReport": "昨天数据较前天增长约 9.1%，整体呈上升趋势..."
}
```

## 数据查询模式

### 模式一：ClickHouse JDBC 直连（默认）

- `app.mcp.enabled=false` 或未配置
- 使用 `ClickHouseJdbcDataQueryService`
- 需配置 `clickhouse.url` 等
- 适合生产环境，延迟低、可控

### 模式二：MCP 调用 ClickHouse MCP Server

- `app.mcp.enabled=true`
- 使用 `McpDataQueryService`
- 需单独启动 ClickHouse MCP Server（如 `npx @clickhouse/mcp-server`）
- 适合与 AI Agent 等 MCP 生态集成

## 自定义 ClickHouse MCP 开发

若需自研 ClickHouse MCP Server，可参考：

1. **MCP 规范**：https://modelcontextprotocol.io/
2. **Java MCP SDK（服务端）**：https://github.com/modelcontextprotocol/java-sdk
3. **官方 ClickHouse MCP**：https://github.com/ClickHouse/mcp-server-clickhouse（Node.js）

建议实现的工具：

- `list_databases`：列出数据库
- `list_tables`：列出表
- `run_select_query`：执行 SELECT 查询

本项目的 `McpDataQueryService` 会调用 `run_select_query` 执行 SQL。

## 扩展说明

1. **表结构适配**：在 `ClickHouseJdbcDataQueryService` 中修改 SQL，以匹配实际表结构。
2. **指标字段**：通过 `DataAnalysisRequest.metricColumns` 指定要分析的指标。
3. **AI 模型**：可在 `application.yml` 中调整 `spring.ai.openai.chat.options.model`（如 `gpt-4o`）。

## License

MIT
