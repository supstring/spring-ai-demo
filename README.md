# Spring AI Demo（双服务：AI Client + MCP Server）

本项目包含两个核心服务：

- `ai-data-analysis`：面向前端的 AI 问答服务（客户端）
- `mediation-report-mcp-server`：MCP 服务，向大模型暴露 `query_mediation_report` 工具

大模型在回答问题时，可以通过 MCP 工具实时查询你现有的广告报表接口数据。

---

## 1. 架构与职责

### `ai-data-analysis`（端口 `8080`）

- 提供问答接口：`POST /api/test/ask-with-mcp`
- 提供测试页面：`/test-ask-with-mcp.html`
- 负责：
  - 接收用户提问
  - 调用大模型（支持动态 `baseUrl/apiKey/model`）
  - 通过 MCP Client 连接 MCP Server 并自动使用工具
  - 维护内存会话上下文（`sessionId`）

### `mediation-report-mcp-server`（端口 `10014`）

- 暴露 MCP SSE 端点：
  - `GET /sse`（握手）
  - `POST /mcp/message`（消息）
- 注册工具：`query_mediation_report`
- 负责把工具调用转发到内部接口：
  - `POST http://localhost:10013/mediation/report/sdk_developer_list_new`

---

## 2. 启动前准备

- JDK 17+
- Maven 3.9+
- 可用的大模型 API Key（如 DeepSeek / DashScope / OpenAI 兼容接口）
- 内部报表服务可访问（默认 `localhost:10013`）

可通过环境变量覆盖关键配置：

- `MEDIATION_MCP_SSE_URL`（默认 `http://localhost:10014/sse`）
- `DEEPSEEK_API_KEY`（`ai-data-analysis` 默认模型 key）

---

## 3. 启动步骤（必须按顺序）

### 第一步：启动 MCP Server

```bash
cd mediation-report-mcp-server
mvn spring-boot:run
```

启动后可检查：

- `http://localhost:10014/sse`（不应是 404）

### 第二步：启动 AI Client 服务

```bash
cd ai-data-analysis
mvn spring-boot:run
```

启动后访问：

- 测试页面：`http://localhost:8080/test-ask-with-mcp.html`

---

## 4. 前端页面如何填写

页面字段说明（`test-ask-with-mcp.html`）：

- `问题`：用户自然语言提问（必填）
- `Session ID`：会话 ID（建议固定同一个，用于连续追问记忆）
- `Provider`：可选。仅用于逻辑标识/兼容原路由（如 `qwen`、`deepseek`）
- `Model`：可选。模型名（如 `qwen-plus`、`deepseek-chat`）
- `baseUrl`：可选。动态模型网关地址（OpenAI 兼容）
- `apiKey`：可选。动态模型密钥

### 动态调用任意模型（推荐）

当你填写了 `baseUrl + apiKey` 时，后端会优先使用这组动态参数调用模型，不依赖 `application.yml` 里的固定 provider 配置。

示例（阿里云 DashScope）：

- `baseUrl`：`https://dashscope.aliyuncs.com/compatible-mode`
- `apiKey`：你的 DashScope Key
- `provider`：`qwen`（可填可不填）
- `model`：`qwen-plus`

> 建议 `baseUrl` 不带 `/v1`，避免部分网关路径重复导致 404。

---

## 5. 服务间调用原理（关键）

用户提问后的完整链路如下：

1. 前端调用 `ai-data-analysis` 的 `/api/test/ask-with-mcp`
2. `ai-data-analysis` 组织提示词 + 会话历史（内存）并请求大模型
3. 大模型判断问题涉及广告指标/分组统计时，触发 MCP 工具调用
4. `ai-data-analysis` 内置 MCP Client 通过 SSE 连接 `mediation-report-mcp-server`
5. `mediation-report-mcp-server` 执行 `query_mediation_report`
6. 工具将参数转发到内部接口 `sdk_developer_list_new`
7. 内部接口返回报表数据，MCP Server 回传给模型
8. 模型基于真实数据生成最终回答，返回前端

---

## 6. `query_mediation_report` 工具规则

工具调用时遵循：

1. 必须提供时间范围（`startDate`、`endDate`）
2. 未指定分页默认：`pageNo=1`、`pageSize=20`
3. 未指定聚合默认：`aggregateType=2`（按天）
4. 未指定 `breakDowns`：返回整体统计
5. `indicators` 至少一个，且应使用系统指标名（如收入使用 `eincome`）

---

## 7. 常见问题排查

### 1) MCP 连接失败（404）

- 先确认 `mediation-report-mcp-server` 已启动
- 检查 `http://localhost:10014/sse` 是否可达
- 检查 `ai-data-analysis` 中的 `MEDIATION_MCP_SSE_URL`

### 2) 模型报 `402 Insufficient Balance`

- 模型账户余额不足，与 MCP 无关
- 更换可用 key，或切换 provider/model

### 3) 动态模型调用 404

- 优先检查 `baseUrl` 是否正确
- 对 DashScope 建议用：`https://dashscope.aliyuncs.com/compatible-mode`

---

## 8. 目录说明

```text
spring-ai-demo/
├── ai-data-analysis/              # 前端对接的 AI 服务（Client）
└── mediation-report-mcp-server/   # MCP 工具服务（Server）
```

