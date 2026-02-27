package com.example.aianalysis.config;

import com.example.aianalysis.skill.Skill;
import com.example.aianalysis.skill.SkillExecutor;
import com.example.aianalysis.skill.impl.DataAnalysisSkill;
import com.example.aianalysis.skill.impl.ReportGenerationSkill;
import com.example.aianalysis.skill.impl.SqlGenerationSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Skill 配置类
 * 
 * 自动注册所有 Skill 到 SkillExecutor
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SkillConfig {

    private final SkillExecutor skillExecutor;
    private final SqlGenerationSkill sqlGenerationSkill;
    private final DataAnalysisSkill dataAnalysisSkill;
    private final ReportGenerationSkill reportGenerationSkill;

    /**
     * 应用启动时注册所有 Skill
     */
    @PostConstruct
    public void registerSkills() {
        log.info("开始注册 Skills...");

        List<Skill<?, ?>> skills = List.of(
                sqlGenerationSkill,
                dataAnalysisSkill,
                reportGenerationSkill
        );

        skills.forEach(skillExecutor::registerSkill);

        log.info("Skills 注册完成，共 {} 个", skills.size());
    }
}
