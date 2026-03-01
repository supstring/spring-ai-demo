package com.example.aianalysis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 模型配置。
 *
 * 支持：
 * 1. default-provider + providers.*：多 provider（各自 base-url/api-key/default-model）
 * 2. default-model/model-aliases：兼容旧版单 provider 模型配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiModelProperties {

    /**
     * 默认 provider 名称（如 deepseek、openai、qwen）。
     */
    private String defaultProvider = "deepseek";

    /**
     * provider 配置集合。
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    /**
     * 兼容旧配置：默认模型（当 providers 未配置时使用）。
     */
    private String defaultModel = "deepseek-chat";

    /**
     * 兼容旧配置：全局模型别名（当 providers 未配置时使用）。
     */
    private Map<String, String> modelAliases = new HashMap<>();

    public String resolveProvider(String requestedProvider) {
        if (!StringUtils.hasText(requestedProvider)) {
            return defaultProvider;
        }
        return requestedProvider.trim();
    }

    public boolean hasProvider(String provider) {
        return providers.containsKey(provider);
    }

    public ProviderConfig getProvider(String provider) {
        return providers.get(provider);
    }

    public String resolveModel(String provider, String requestedModel) {
        ProviderConfig config = providers.get(provider);
        if (config == null) {
            return resolveLegacyModel(requestedModel);
        }
        String candidate = StringUtils.hasText(requestedModel) ? requestedModel.trim() : config.getDefaultModel();
        if (!StringUtils.hasText(candidate)) {
            return defaultModel;
        }
        return config.getModelAliases().getOrDefault(candidate, candidate);
    }

    public String resolveLegacyModel(String requestedModel) {
        String candidate = StringUtils.hasText(requestedModel) ? requestedModel.trim() : defaultModel;
        return modelAliases.getOrDefault(candidate, candidate);
    }

    @Data
    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
        private String defaultModel;
        private Map<String, String> modelAliases = new HashMap<>();
    }
}
