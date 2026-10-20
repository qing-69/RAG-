package com.xiaoyan.aiassistant.gap;

import java.time.LocalDateTime;
import java.util.Map;

// 知识缺口分析摘要，展示提问规模和低置信占比。
public record KnowledgeGapSummary(
        long totalQuestions,
        long lowConfidenceCount,
        double lowConfidenceRatio,
        Map<String, Long> levelDistribution,
        LocalDateTime since
) {
}
