package com.example.aianalysis.controller;

import com.example.aianalysis.dto.SimpleAskRequest;
import com.example.aianalysis.service.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;


    @PostMapping(value = "/simple-ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> simpleAsk(@RequestBody(required = false) @Valid SimpleAskRequest request) {
        String response = testService.simpleAsk(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/simple-ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> simpleAskStream(@Valid @ModelAttribute SimpleAskRequest request) {
        if (request == null || request.getProblem() == null || request.getProblem().isBlank()) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("problem 不能为空")
                    .build());
        }
        return testService.simpleAskStream(request)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(chunk)
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()));
    }

    @PostMapping(value = "/ask-with-mcp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> askWithMcp(@RequestBody(required = false) @Valid SimpleAskRequest request) {
        String response = testService.askWithMcp(request);
        return ResponseEntity.ok(response);
    }

}
