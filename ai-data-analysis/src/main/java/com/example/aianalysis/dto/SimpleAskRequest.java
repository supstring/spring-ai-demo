package com.example.aianalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleAskRequest {

    private String problem;
    /**
     * 可选：会话ID。相同 sessionId 会共享内存中的上下文，用于连续追问。
     */
    private String sessionId;

    /**
     * 可选：指定 AI 模型名或别名（如 deepseek、deepseek-chat、deepseek-reasoner）
     */
    private String modelName;

    /**
     * 可选：指定 AI 提供商（如 deepseek、openai、qwen）
     */
    private String provider;

    /**
     * 可选：动态指定模型服务 baseUrl（OpenAI 兼容格式）。
     */
    private String baseUrl;

    /**
     * 可选：动态指定模型服务 apiKey（OpenAI 兼容格式）。
     */
    private String apiKey;
}
