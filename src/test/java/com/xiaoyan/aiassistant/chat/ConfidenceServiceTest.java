package com.xiaoyan.aiassistant.chat;

import com.xiaoyan.aiassistant.config.AppProperties;
import com.xiaoyan.aiassistant.retrieval.RetrievalCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ConfidenceService 的规则打分测试。
class ConfidenceServiceTest {

    private final ConfidenceService confidenceService = new ConfidenceService(new AppProperties());

    // 空检索结果判为低置信。
    @Test
    void assessesEmptyKnowledgeAsLow() {
        ConfidenceReport report = confidenceService.assess(List.of());
        assertThat(report.level()).isEqualTo(ConfidenceReport.LEVEL_LOW);
        assertThat(report.score()).isEqualTo(0.0);
        assertThat(report.reason()).contains("没有检索到");
        assertThat(report.knowledgeCount()).isZero();
    }

    // 高分且多候选判为高置信。
    @Test
    void assessesHighScoreMultiCandidateAsHigh() {
        ConfidenceReport report = confidenceService.assess(List.of(
                candidate(0.92, 0),
                candidate(0.88, 0)
        ));
        assertThat(report.level()).isEqualTo(ConfidenceReport.LEVEL_HIGH);
        assertThat(report.topVectorScore()).isEqualTo(0.92);
        assertThat(report.knowledgeCount()).isEqualTo(2);
    }

    // 高分但只有单候选降级为中置信。
    @Test
    void assessesSingleHighScoreCandidateAsMedium() {
        ConfidenceReport report = confidenceService.assess(List.of(candidate(0.95, 0)));
        assertThat(report.level()).isEqualTo(ConfidenceReport.LEVEL_MEDIUM);
        assertThat(report.reason()).contains("仅检索到 1 个");
    }

    // 中等分数判为中置信。
    @Test
    void assessesMediumScoreAsMedium() {
        ConfidenceReport report = confidenceService.assess(List.of(
                candidate(0.72, 0),
                candidate(0.68, 0)
        ));
        assertThat(report.level()).isEqualTo(ConfidenceReport.LEVEL_MEDIUM);
    }

    // 低于中置信阈值判为低置信。
    @Test
    void assessesLowScoreAsLow() {
        ConfidenceReport report = confidenceService.assess(List.of(candidate(0.55, 0)));
        assertThat(report.level()).isEqualTo(ConfidenceReport.LEVEL_LOW);
        assertThat(report.reason()).contains("偏低");
    }

    // 仅 BM25 命中（无向量分）判为中置信。
    @Test
    void assessesKeywordOnlyHitAsMedium() {
        ConfidenceReport report = confidenceService.assess(List.of(candidate(0.0, 3.2)));
        assertThat(report.level()).isEqualTo(ConfidenceReport.LEVEL_MEDIUM);
        assertThat(report.reason()).contains("关键词");
    }

    // 既无向量命中也无关键词命中判为低置信。
    @Test
    void assessesNoSignalAsLow() {
        ConfidenceReport report = confidenceService.assess(List.of(candidate(0.0, 0.0)));
        assertThat(report.level()).isEqualTo(ConfidenceReport.LEVEL_LOW);
    }

    // 构造一个指定分数的候选片段。
    private RetrievalCandidate candidate(double vectorScore, double keywordScore) {
        return new RetrievalCandidate("vector-1", 1L, 1L, "标题", "doc.md", "内容", vectorScore, keywordScore, 0.0);
    }
}
