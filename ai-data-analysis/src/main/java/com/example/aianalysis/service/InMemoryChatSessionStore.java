package com.example.aianalysis.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryChatSessionStore {

    private static final int MAX_TURNS_PER_SESSION = 20;

    private final Map<String, Deque<Turn>> sessionTurns = new ConcurrentHashMap<>();

    public String normalizeSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return "default-session";
        }
        return sessionId.trim();
    }

    public String buildHistoryText(String sessionId) {
        Deque<Turn> turns = sessionTurns.get(normalizeSessionId(sessionId));
        if (turns == null || turns.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (Turn turn : turns) {
            builder.append("用户: ").append(turn.userQuestion()).append("\n");
            builder.append("助手: ").append(turn.assistantAnswer()).append("\n");
        }
        return builder.toString().trim();
    }

    public void appendTurn(String sessionId, String userQuestion, String assistantAnswer) {
        String key = normalizeSessionId(sessionId);
        Deque<Turn> turns = sessionTurns.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (turns) {
            turns.addLast(new Turn(userQuestion, assistantAnswer));
            while (turns.size() > MAX_TURNS_PER_SESSION) {
                turns.removeFirst();
            }
        }
    }

    private record Turn(String userQuestion, String assistantAnswer) {
    }
}
