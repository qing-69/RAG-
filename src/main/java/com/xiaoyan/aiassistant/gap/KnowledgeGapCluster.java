package com.xiaoyan.aiassistant.gap;

import java.time.LocalDateTime;
import java.util.List;

// 一个缺口主题：同类低置信问题的聚合结果。
public record KnowledgeGapCluster(
        String representativeQuestion,
        List<String> keywords,
        int questionCount,
        double averageTopVectorScore,
        List<String> sampleQuestions,
        LocalDateTime lastAskedAt
) {
}
