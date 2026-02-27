package com.example.aianalysis.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 执行器
 * 
 * 负责管理和执行各种 Skill，支持：
 * 1. Skill 注册与发现
 * 2. 链式 Skill 执行
 * 3. 条件 Skill 执行
 */
@Slf4j
@Component
public class SkillExecutor {

    private final Map<String, Skill<?, ?>> skillRegistry = new ConcurrentHashMap<>();

    /**
     * 注册 Skill
     */
    public void registerSkill(Skill<?, ?> skill) {
        skillRegistry.put(skill.getName(), skill);
        log.info("Skill 已注册: {}", skill.getName());
    }

    /**
     * 获取 Skill
     */
    @SuppressWarnings("unchecked")
    public <I, O> Skill<I, O> getSkill(String name) {
        return (Skill<I, O>) skillRegistry.get(name);
    }

    /**
     * 执行单个 Skill
     */
    @SuppressWarnings("unchecked")
    public <I, O> O execute(String skillName, I input, SkillContext context) {
        Skill<I, O> skill = (Skill<I, O>) skillRegistry.get(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("Skill not found: " + skillName);
        }

        log.info("执行 Skill: {}, 输入: {}", skillName, input);
        long startTime = System.currentTimeMillis();

        try {
            O result = skill.execute(input);
            context.recordStep(skillName, result);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Skill 执行完成: {}, 耗时: {}ms", skillName, duration);

            return result;
        } catch (Exception e) {
            log.error("Skill 执行失败: {}", skillName, e);
            throw new SkillExecutionException("Skill execution failed: " + skillName, e);
        }
    }

    /**
     * 链式执行多个 Skill
     * 
     * @param chain Skill 链定义
     * @param initialInput 初始输入
     * @param context 执行上下文
     * @return 最终结果
     */
    @SuppressWarnings("unchecked")
    public <O> O executeChain(SkillChain chain, Object initialInput, SkillContext context) {
        Object currentInput = initialInput;

        for (SkillChain.Step step : chain.getSteps()) {
            Skill<Object, Object> skill = (Skill<Object, Object>) skillRegistry.get(step.getSkillName());
            if (skill == null) {
                throw new IllegalArgumentException("Skill not found: " + step.getSkillName());
            }

            // 应用输入转换
            if (step.getInputTransformer() != null) {
                currentInput = step.getInputTransformer().transform(currentInput, context);
            }

            // 执行 Skill
            Object result = skill.execute(currentInput);
            context.recordStep(step.getSkillName(), result);

            // 应用输出转换
            if (step.getOutputTransformer() != null) {
                currentInput = step.getOutputTransformer().transform(result, context);
            } else {
                currentInput = result;
            }
        }

        return (O) currentInput;
    }

    /**
     * 获取所有已注册的 Skill
     */
    public List<Skill<?, ?>> getAllSkills() {
        return new ArrayList<>(skillRegistry.values());
    }

    /**
     * Skill 执行异常
     */
    public static class SkillExecutionException extends RuntimeException {
        public SkillExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
