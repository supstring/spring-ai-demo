# Mediation Report MCP Server

该模块提供一个 MCP Server，暴露工具 `query_mediation_report`，用于把 Agent 请求转发到内部接口：

- `POST http://localhost:10013/mediation/report/sdk_developer_list_new`

## 运行方式

```bash
cd mediation-report-mcp-server
mvn spring-boot:run
```

默认监听端口：`10014`

## MCP 端点

- SSE 握手端点：`/sse`
- 消息端点：`/mcp/message`

## 工具：query_mediation_report

### 行为规则

1. 必须提供时间范围：`startDate` 和 `endDate`
2. 若未指定分页，默认：
   - `pageNo=1`
   - `pageSize=20`
3. 若未指定聚合方式，默认 `aggregateType=2`（按天）
4. 若未指定 `breakDowns`，返回整体统计（空数组）
5. `indicators` 至少包含一个指标

### 典型问题

- “昨天 AU 国家 BigoDsp 的收益是多少”
- “按国家统计最近一天的 ecpm”
- “查询某个 appKey 的广告表现”
- “按 dspName 分组看收入”

## 配置项

`src/main/resources/application.yml`:

```yaml
server:
  port: 10014

app:
  backend:
    base-url: http://localhost:10013
    report-path: /mediation/report/sdk_developer_list_new
```
