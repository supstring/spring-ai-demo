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
    private final Map<String, ChatClient> dynamicClientCache = new ConcurrentHashMap<>();

    public ResolvedTarget resolve(String requestedProvider, String requestedModel) {
        String provider = aiModelProperties.resolveProvider(requestedProvider);
        String model = aiModelProperties.resolveModel(provider, requestedModel);
        ChatClient chatClient = resolveClient(provider);
        return new ResolvedTarget(provider, model, chatClient);
    }

    public ResolvedTarget resolve(String requestedProvider, String requestedModel, String dynamicBaseUrl, String dynamicApiKey) {
        boolean hasBaseUrl = StringUtils.hasText(dynamicBaseUrl);
        boolean hasApiKey = StringUtils.hasText(dynamicApiKey);
        if (hasBaseUrl || hasApiKey) {
            if (!hasBaseUrl || !hasApiKey) {
                throw new IllegalArgumentException("当传入动态模型配置时，baseUrl 和 apiKey 必须同时提供");
            }
            String provider = StringUtils.hasText(requestedProvider) ? requestedProvider.trim() : "dynamic";
            String model = StringUtils.hasText(requestedModel)
                    ? requestedModel.trim()
                    : aiModelProperties.resolveModel(aiModelProperties.resolveProvider(requestedProvider), null);
            ChatClient chatClient = resolveDynamicClient(dynamicBaseUrl.trim(), dynamicApiKey.trim(), model);
            return new ResolvedTarget(provider, model, chatClient);
        }
        return resolve(requestedProvider, requestedModel);
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

        OpenAiApi openAiApi = new OpenAiApi.Builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .build();
        OpenAiChatOptions defaultOptions = OpenAiChatOptions.builder()
                .model(aiModelProperties.resolveModel(provider, null))
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(defaultOptions)
                .build();

        log.info("初始化 AI Provider 客户端: {}", provider);
        return ChatClient.create(chatModel);
    }

    private ChatClient resolveDynamicClient(String baseUrl, String apiKey, String model) {
        String cacheKey = baseUrl + "|" + apiKey + "|" + model;
        return dynamicClientCache.computeIfAbsent(cacheKey, ignored -> createDynamicClient(baseUrl, apiKey, model));
    }

    private ChatClient createDynamicClient(String baseUrl, String apiKey, String model) {
        OpenAiApi openAiApi = new OpenAiApi.Builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions defaultOptions = OpenAiChatOptions.builder()
                .model(model)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(defaultOptions)
                .build();
        log.info("初始化动态 AI 客户端, baseUrl: {}, model: {}", baseUrl, model);
        return ChatClient.create(chatModel);
    }

    public record ResolvedTarget(String provider, String model, ChatClient chatClient) {
    }
}
