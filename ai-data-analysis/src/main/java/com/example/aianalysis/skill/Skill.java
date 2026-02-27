package com.example.aianalysis.skill;

import java.util.Map;

/**
 * Skill 接口 - AI Agent 的核心能力单元
 * 
 * 每个 Skill 代表一个特定的能力，如：
 * - 数据查询 Skill
 * - 数据分析 Skill
 * - SQL 生成 Skill
 * - 报告生成 Skill
 * 
 * @param <I> 输入类型
 * @param <O> 输出类型
 */
public interface Skill<I, O> {

    /**
     * Skill 的唯一标识名
     */
    String getName();

    /**
     * Skill 的描述，用于 AI 理解该能力
     */
    String getDescription();

    /**
     * 执行 Skill
     * 
     * @param input 输入参数
     * @return 执行结果
     */
    O execute(I input);

    /**
     * 获取输入参数的 Schema 描述（用于 AI 理解参数结构）
     */
    default Map<String, Object> getInputSchema() {
        return Map.of();
    }

    /**
     * 获取输出结果的 Schema 描述
     */
    default Map<String, Object> getOutputSchema() {
        return Map.of();
    }
}
