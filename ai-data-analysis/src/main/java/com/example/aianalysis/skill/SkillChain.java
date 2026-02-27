package com.example.aianalysis.skill;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Skill 链定义
 * 
 * 用于编排多个 Skill 的执行顺序和数据流转
 */
@Data
@Builder
public class SkillChain {

    private String name;
    private String description;

    @Singular
    private List<Step> steps;

    /**
     * Skill 链中的执行步骤
     */
    @Data
    @Builder
    public static class Step {
        /**
         * Skill 名称
         */
        private String skillName;

        /**
         * 步骤描述
         */
        private String description;

        /**
         * 输入数据转换器
         */
        private DataTransformer inputTransformer;

        /**
         * 输出数据转换器
         */
        private DataTransformer outputTransformer;

        /**
         * 执行条件（可选）
         */
        private Condition condition;
    }

    /**
     * 数据转换器接口
     */
    @FunctionalInterface
    public interface DataTransformer {
        Object transform(Object input, SkillContext context);
    }

    /**
     * 条件接口
     */
    @FunctionalInterface
    public interface Condition {
        boolean evaluate(SkillContext context);
    }
}
