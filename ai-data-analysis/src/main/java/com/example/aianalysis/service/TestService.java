package com.example.aianalysis.service;

import com.example.aianalysis.config.AiClientRouter;
import com.example.aianalysis.dto.SimpleAskRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final AiClientRouter aiClientRouter;
    private final ToolCallbackProvider toolCallbackProvider;
    private final InMemoryChatSessionStore chatSessionStore;



    public String simpleAsk(SimpleAskRequest request) {

        AiClientRouter.ResolvedTarget target = aiClientRouter.resolve(
                request.getProvider(),
                request.getModelName(),
                request.getBaseUrl(),
                request.getApiKey()
        );
        String prompt = request.getProblem();

        try {
            return target.chatClient()
                    .prompt()
                    .user(prompt)
                    .options(OpenAiChatOptions.builder().model(target.model()).build())
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("调用AI回答失败: {}", e.getMessage());
            return buildFriendlyErrorMessage(e, "调用AI回答失败");
        }
    }

    public Flux<String> simpleAskStream(SimpleAskRequest request) {
        AiClientRouter.ResolvedTarget target = aiClientRouter.resolve(
                request.getProvider(),
                request.getModelName(),
                request.getBaseUrl(),
                request.getApiKey()
        );
        String prompt = request.getProblem();

        return target.chatClient()
                .prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(target.model()).build())
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.warn("流式调用AI回答失败: {}", e.getMessage());
                    return Flux.just(buildFriendlyErrorMessage(e, "调用AI回答失败"));
                });
    }

    public String askWithMcp(SimpleAskRequest request) {
        if (request == null || !StringUtils.hasText(request.getProblem())) {
            return "problem 不能为空";
        }

        AiClientRouter.ResolvedTarget target = aiClientRouter.resolve(
                request.getProvider(),
                request.getModelName(),
                request.getBaseUrl(),
                request.getApiKey()
        );
        String prompt = request.getProblem();
        String sessionId = chatSessionStore.normalizeSessionId(request.getSessionId());
        String history = chatSessionStore.buildHistoryText(sessionId);
        String userInput = StringUtils.hasText(history)
                ? """
                下面是当前会话的历史对话，请结合历史信息继续回答。
                历史对话：
                %s

                当前用户问题：
                %s
                """.formatted(history, prompt)
                : prompt;

        try {
            String answer = target.chatClient()
                    .prompt()
                    .system("""
                            你是广告数据分析助手。
                            当用户问题涉及广告收益/请求/展示/点击/ecpm，或按国家、开发者、广告类型、DSP、appKey分组统计时，
                            请主动调用 query_mediation_report 工具查询真实数据，再据此回答。
                            调用工具时请遵循：
                            1) 必须提供 timeZone、startDate、endDate。
                            2) indicators 至少一个。
                            3) 用户未指定分页默认 pageNo=1,pageSize=20。
                            4) 用户未指定聚合方式默认 aggregateType=2。
                            5) 用户未指定 breakDowns 则整体统计。
                            6) 指标字段必须使用系统指标名，例如：收入/收益/revenue -> eincome。
                            若用户问题缺少必要时间条件，先询问或给出默认时间建议，不要编造数据。
                            """)
                    .user(userInput)
                    .toolCallbacks(toolCallbackProvider)
                    .options(OpenAiChatOptions.builder().model(target.model()).build())
                    .call()
                    .content();
            chatSessionStore.appendTurn(sessionId, prompt, answer);
            return answer;
        } catch (Exception e) {
            log.warn("调用AI(MCP)回答失败: {}", e.getMessage());
            return buildFriendlyErrorMessage(e, "调用AI(MCP)回答失败");
        }
    }

    private String buildFriendlyErrorMessage(Throwable throwable, String fallback) {
        if (throwable == null) {
            return fallback;
        }
        String message = throwable.getMessage();
        if (!StringUtils.hasText(message)) {
            return fallback;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("insufficient balance") || (normalized.contains("402") && normalized.contains("balance"))) {
            return "模型服务余额不足（402），请充值后重试，或切换到其它可用 provider/model。";
        }
        if (normalized.contains("invalid api key") || normalized.contains("authentication")) {
            return "模型服务鉴权失败，请检查 API Key 是否正确且未过期。";
        }
        if (normalized.contains("rate limit") || normalized.contains("too many requests") || normalized.contains("429")) {
            return "模型请求过于频繁（429），请稍后重试。";
        }
        return fallback + "，错误详情：" + message;
    }
}
