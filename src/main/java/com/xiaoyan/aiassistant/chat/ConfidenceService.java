package com.xiaoyan.aiassistant.chat;

import com.xiaoyan.aiassistant.config.AppProperties;
import com.xiaoyan.aiassistant.retrieval.RetrievalCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

// 基于检索分数的规则置信度评估，不调用大模型。
@Service
@RequiredArgsConstructor
public class ConfidenceService {

    private final AppProperties properties;

    // 只依据知识库候选打分，长期记忆不计入置信度。
    public ConfidenceReport assess(List<RetrievalCandidate> knowledge) {
        if (knowledge == null || knowledge.isEmpty()) {
            return new ConfidenceReport(ConfidenceReport.LEVEL_LOW, 0.0, "知识库中没有检索到相关片段", 0, 0.0);
        }

        int count = knowledge.size();
        double topVectorScore = knowledge.stream()
                .mapToDouble(RetrievalCandidate::getVectorScore)
                .max()
                .orElse(0.0);
        boolean keywordHit = knowledge.stream().anyMatch(candidate -> candidate.getKeywordScore() > 0);
        AppProperties.Confidence config = properties.getConfidence();

        if (topVectorScore <= 0 && !keywordHit) {
            return new ConfidenceReport(ConfidenceReport.LEVEL_LOW, 0.0, "检索片段相关度偏低", count, topVectorScore);
        }
        if (topVectorScore <= 0) {
            return new ConfidenceReport(ConfidenceReport.LEVEL_MEDIUM, 0.5, "仅关键词命中，语义相关度未知", count, topVectorScore);
        }
        if (topVectorScore >= config.getHighScoreThreshold() && count >= config.getMinHighCount()) {
            return new ConfidenceReport(ConfidenceReport.LEVEL_HIGH, topVectorScore,
                    "检索片段相关度高(" + format(topVectorScore) + ")，命中 " + count + " 个片段", count, topVectorScore);
        }
        if (topVectorScore >= config.getHighScoreThreshold()) {
            return new ConfidenceReport(ConfidenceReport.LEVEL_MEDIUM, topVectorScore,
                    "仅检索到 " + count + " 个相关片段", count, topVectorScore);
        }
        if (topVectorScore >= config.getMediumScoreThreshold()) {
            return new ConfidenceReport(ConfidenceReport.LEVEL_MEDIUM, topVectorScore,
                    "检索片段相关度中等(" + format(topVectorScore) + ")", count, topVectorScore);
        }
        return new ConfidenceReport(ConfidenceReport.LEVEL_LOW, topVectorScore,
                "检索片段相关度偏低(" + format(topVectorScore) + ")", count, topVectorScore);
    }

    // 统一保留两位小数，便于前端展示。
    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
