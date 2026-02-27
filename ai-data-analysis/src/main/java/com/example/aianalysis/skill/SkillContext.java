package com.example.aianalysis.skill;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill 执行上下文
 * 
 * 用于在 Skill 之间传递状态和数据
 */
@Data
@Builder
public class SkillContext {

    /**
     * 上下文数据存储
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    /**
     * 执行历史记录
     */
    @Builder.Default
    private Map<String, Object> executionHistory = new HashMap<>();

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 获取上下文中的数据
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /**
     * 设置上下文数据
     */
    public void set(String key, Object value) {
        data.put(key, value);
    }

    /**
     * 记录执行步骤
     */
    public void recordStep(String stepName, Object result) {
        executionHistory.put(stepName, result);
    }
}
