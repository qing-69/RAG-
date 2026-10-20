package com.xiaoyan.aiassistant.gap;

import com.xiaoyan.aiassistant.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// KnowledgeGapService 规则聚类逻辑测试。
class KnowledgeGapServiceTest {

    private final KnowledgeGapService knowledgeGapService =
            new KnowledgeGapService(null, new AppProperties());

    // 含共同关键词的问题聚为同一主题。
    @Test
    void groupsSimilarQuestionsIntoSameCluster() {
        List<ChatQueryLog> logs = List.of(
                log("Redis 缓存穿透怎么解决？缓存 穿透", 0.5),
                log("Redis 缓存穿透和缓存击穿的区别？缓存 穿透", 0.4),
                log("Python 装饰器怎么写？装饰器 语法", 0.3)
        );

        List<KnowledgeGapCluster> clusters = knowledgeGapService.clusterLogs(logs);

        assertThat(clusters).hasSize(2);
        assertThat(clusters.get(0).questionCount()).isEqualTo(2);
        assertThat(clusters.get(0).keywords()).contains("缓存", "穿透");
        assertThat(clusters.get(1).questionCount()).isEqualTo(1);
    }

    // 无关问题各开新组，按提问次数降序。
    @Test
    void sortsClustersByQuestionCountDescending() {
        List<ChatQueryLog> logs = List.of(
                log("K8s 滚动更新策略？滚动 更新", 0.4),
                log("K8s 滚动更新回滚怎么做？滚动 更新", 0.3),
                log("K8s 滚动更新超时怎么处理？滚动 更新", 0.3),
                log("MySQL 索引失效场景？索引 失效", 0.5)
        );

        List<KnowledgeGapCluster> clusters = knowledgeGapService.clusterLogs(logs);

        assertThat(clusters).hasSize(2);
        assertThat(clusters.get(0).questionCount()).isEqualTo(3);
        assertThat(clusters.get(1).questionCount()).isEqualTo(1);
    }

    // Jaccard 相似度边界：无交集为 0，完全相同为 1。
    @Test
    void computesJaccardSimilarity() {
        assertThat(knowledgeGapService.jaccard(Set.of("a", "b"), Set.of("c", "d"))).isEqualTo(0.0);
        assertThat(knowledgeGapService.jaccard(Set.of("a", "b"), Set.of("a", "b"))).isEqualTo(1.0);
        assertThat(knowledgeGapService.jaccard(Set.of("a", "b"), Set.of("b", "c"))).isEqualTo(1.0 / 3.0);
        assertThat(knowledgeGapService.jaccard(Set.of(), Set.of("a"))).isEqualTo(0.0);
    }

    // 关键词抽取对空白问题返回空集合。
    @Test
    void extractsEmptyKeywordsForBlankQuestion() {
        assertThat(knowledgeGapService.extractKeywords("  ", 5)).isEmpty();
        assertThat(knowledgeGapService.extractKeywords(null, 5)).isEmpty();
    }

    // 构造一条问答日志，关键词由问题文本抽取。
    private ChatQueryLog log(String question, double topVectorScore) {
        return new ChatQueryLog(
                null, "default", "s1", question, question, "", "",
                1, topVectorScore, 0, "LOW", 0.2, "测试", LocalDateTime.now()
        );
    }
}
