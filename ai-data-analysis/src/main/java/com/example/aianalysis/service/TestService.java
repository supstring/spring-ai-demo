package com.example.aianalysis.service;

import com.example.aianalysis.config.AiClientRouter;
import com.example.aianalysis.dto.SimpleAskRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final AiClientRouter aiClientRouter;



    public String simpleAsk(SimpleAskRequest request) {

        AiClientRouter.ResolvedTarget target = aiClientRouter.resolve(request.getProvider(), request.getModelName());
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
            return "调用AI回答失败";
        }
    }

    public Flux<String> simpleAskStream(SimpleAskRequest request) {
        AiClientRouter.ResolvedTarget target = aiClientRouter.resolve(request.getProvider(), request.getModelName());
        String prompt = request.getProblem();

        return target.chatClient()
                .prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(target.model()).build())
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.warn("流式调用AI回答失败: {}", e.getMessage());
                    return Flux.just("调用AI回答失败");
                });
    }
}
