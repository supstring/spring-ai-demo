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
     * 可选：指定 AI 模型名或别名（如 deepseek、deepseek-chat、deepseek-reasoner）
     */
    private String modelName;

    /**
     * 可选：指定 AI 提供商（如 deepseek、openai、qwen）
     */
    private String provider;
}
