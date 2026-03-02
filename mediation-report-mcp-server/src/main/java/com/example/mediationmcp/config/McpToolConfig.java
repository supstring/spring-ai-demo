package com.example.mediationmcp.config;

import com.example.mediationmcp.tool.MediationReportTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider mediationToolCallbackProvider(MediationReportTool mediationReportTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(mediationReportTool)
                .build();
    }
}
