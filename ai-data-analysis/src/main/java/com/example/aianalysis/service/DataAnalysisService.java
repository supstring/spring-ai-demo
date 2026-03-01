package com.example.aianalysis.service;

import com.example.aianalysis.config.AiClientRouter;
import com.example.aianalysis.dto.DataAnalysisRequest;
import com.example.aianalysis.dto.DataAnalysisResponse;
import com.example.aianalysis.dto.DataAnalysisResponse.MetricChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据分析编排服务：查询数据 + AI 分析 + 生成 JSON 报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataAnalysisService {

    private final DataQueryService dataQueryService;
    private final AiClientRouter aiClientRouter;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public DataAnalysisResponse analyzeYesterdayVsDayBefore(DataAnalysisRequest request) {
        String yesterday = LocalDate.now().minusDays(1).format(DATE_FORMAT);
        String dayBefore = LocalDate.now().minusDays(2).format(DATE_FORMAT);

        try {
            Map<String, Object> yesterdayData = dataQueryService.queryDataSummary(yesterday, request);
            Map<String, Object> dayBeforeData = dataQueryService.queryDataSummary(dayBefore, request);

            List<MetricChange> changes = computeMetricChanges(yesterdayData, dayBeforeData);
            String analysisReport = generateAnalysisReport(
                    yesterday, dayBefore, yesterdayData, dayBeforeData, changes,
                    request.getProvider(), request.getModelName());

            return DataAnalysisResponse.builder()
                    .success(true)
                    .yesterday(yesterday)
                    .dayBeforeYesterday(dayBefore)
                    .yesterdaySummary(yesterdayData)
                    .dayBeforeSummary(dayBeforeData)
                    .metricChanges(changes)
                    .analysisReport(analysisReport)
                    .build();
        } catch (Exception e) {
            log.error("数据分析失败", e);
            return DataAnalysisResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .yesterday(yesterday)
                    .dayBeforeYesterday(dayBefore)
                    .build();
        }
    }

    private List<MetricChange> computeMetricChanges(Map<String, Object> yesterday, Map<String, Object> dayBefore) {
        List<MetricChange> changes = new ArrayList<>();
        for (String key : yesterday.keySet()) {
            if (!dayBefore.containsKey(key)) continue;
            Object yVal = yesterday.get(key);
            Object bVal = dayBefore.get(key);
            if (!(yVal instanceof Number) || !(bVal instanceof Number)) continue;

            double y = ((Number) yVal).doubleValue();
            double b = ((Number) bVal).doubleValue();
            double pct = b == 0 ? (y == 0 ? 0 : 100) : (y - b) / b * 100;
            String trend = pct > 0 ? "up" : pct < 0 ? "down" : "unchanged";

            changes.add(MetricChange.builder()
                    .metricName(key)
                    .yesterdayValue(yVal)
                    .dayBeforeValue(bVal)
                    .changePercent(Math.round(pct * 10) / 10.0)
                    .trend(trend)
                    .build());
        }
        return changes;
    }

    private String generateAnalysisReport(String yesterday, String dayBefore,
                                          Map<String, Object> yesterdayData, Map<String, Object> dayBeforeData,
                                          List<MetricChange> changes,
                                          String requestedProvider,
                                          String requestedModel) {
        AiClientRouter.ResolvedTarget target = aiClientRouter.resolve(requestedProvider, requestedModel);
        String prompt = """
                请根据以下数据，用简洁的中文写一段「昨天相对于前天的数据变化分析」报告（2-5句话）：
                - 昨天(%s)数据：%s
                - 前天(%s)数据：%s
                指标变化：%s
                """.formatted(yesterday, yesterdayData, dayBefore, dayBeforeData, changes);

        try {
            return target.chatClient()
                    .prompt()
                    .user(prompt)
                    .options(OpenAiChatOptions.builder().model(target.model()).build())
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI 报告生成失败，使用默认文案: {}", e.getMessage());
            return "数据已查询完成，指标变化见 metricChanges。AI 分析暂不可用。";
        }
    }
}
