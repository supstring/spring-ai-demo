package com.example.aianalysis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 客户端路由器。
 *
 * 根据 provider 选择对应的 ChatClient，并支持模型别名解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiClientRouter {

    private final AiModelProperties aiModelProperties;
    private final ChatClient.Builder defaultChatClientBuilder;

    private final Map<String, ChatClient> clientCache = new ConcurrentHashMap<>();

    public ResolvedTarget resolve(String requestedProvider, String requestedModel) {
        String provider = aiModelProperties.resolveProvider(requestedProvider);
        String model = aiModelProperties.resolveModel(provider, requestedModel);
        ChatClient chatClient = resolveClient(provider);
        return new ResolvedTarget(provider, model, chatClient);
    }

    private ChatClient resolveClient(String provider) {
        if (!aiModelProperties.hasProvider(provider)) {
            // 未配置 provider 时回退到 Spring AI 默认 ChatClient，兼容旧配置。
            return defaultChatClientBuilder.build();
        }
        return clientCache.computeIfAbsent(provider, this::createProviderClient);
    }

    private ChatClient createProviderClient(String provider) {
        AiModelProperties.ProviderConfig config = aiModelProperties.getProvider(provider);
        if (config == null) {
            return defaultChatClientBuilder.build();
        }
        if (!StringUtils.hasText(config.getBaseUrl()) || !StringUtils.hasText(config.getApiKey())) {
            throw new IllegalArgumentException("AI provider 配置不完整: " + provider + "，请检查 base-url/api-key");
        }

        OpenAiApi openAiApi = new OpenAiApi(config.getBaseUrl(), config.getApiKey());
        OpenAiChatOptions defaultOptions = OpenAiChatOptions.builder()
                .model(aiModelProperties.resolveModel(provider, null))
                .build();
        OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, defaultOptions);

        log.info("初始化 AI Provider 客户端: {}", provider);
        return ChatClient.create(chatModel);
    }

    public record ResolvedTarget(String provider, String model, ChatClient chatClient) {
    }
}
