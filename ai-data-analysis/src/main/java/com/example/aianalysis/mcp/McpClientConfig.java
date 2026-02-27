package com.example.aianalysis.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 客户端配置
 *
 * 配置 MCP 客户端连接到 MCP Server（如 ClickHouse MCP Server）
 *
 * 注意：MCP Java SDK 0.9.0 的 API 仍在快速迭代中，
 * 这里提供配置占位符，实际使用时请参考官方文档：
 * https://github.com/modelcontextprotocol/java-sdk
 */
@Slf4j
@Configuration
public class McpClientConfig {

    @Value("${app.mcp.clickhouse.http-url:http://localhost:3000}")
    private String mcpHttpUrl;

    @Value("${app.mcp.clickhouse.command:npx}")
    private String mcpCommand;

    @Value("${app.mcp.clickhouse.args:-y,@clickhouse/mcp-server}")
    private String mcpArgs;

    /**
     * MCP 配置属性
     */
    public McpProperties getMcpProperties() {
        return McpProperties.builder()
                .httpUrl(mcpHttpUrl)
                .command(mcpCommand)
                .args(mcpArgs)
                .build();
    }

    /**
     * MCP 配置属性类
     */
    public record McpProperties(String httpUrl, String command, String args) {
        public static McpProperties builder() {
            return new McpProperties(null, null, null);
        }

        public McpProperties httpUrl(String httpUrl) {
            return new McpProperties(httpUrl, this.command, this.args);
        }

        public McpProperties command(String command) {
            return new McpProperties(this.httpUrl, command, this.args);
        }

        public McpProperties args(String args) {
            return new McpProperties(this.httpUrl, this.command, args);
        }

        public McpProperties build() {
            return this;
        }
    }
}
