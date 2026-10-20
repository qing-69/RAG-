package com.xiaoyan.aiassistant.gap;

import com.hankcs.hanlp.HanLP;
import com.xiaoyan.aiassistant.chat.ConfidenceReport;
import com.xiaoyan.aiassistant.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 知识缺口分析：统计低置信提问并按关键词规则聚类成缺口主题。
@Service
@RequiredArgsConstructor
public class KnowledgeGapService {

    private final ChatQueryLogMapper chatQueryLogMapper;
    private final AppProperties properties;

    // 汇总时间窗内的提问规模和置信度分布。
    public KnowledgeGapSummary summarize(int days) {
        LocalDateTime since = since(days);
        long total = chatQueryLogMapper.countSince(since);
        long high = chatQueryLogMapper.countByLevelSince(since, ConfidenceReport.LEVEL_HIGH);
        long medium = chatQueryLogMapper.countByLevelSince(since, ConfidenceReport.LEVEL_MEDIUM);
        long low = chatQueryLogMapper.countByLevelSince(since, ConfidenceReport.LEVEL_LOW);
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put(ConfidenceReport.LEVEL_HIGH, high);
        distribution.put(ConfidenceReport.LEVEL_MEDIUM, medium);
        distribution.put(ConfidenceReport.LEVEL_LOW, low);
        double ratio = total == 0 ? 0.0 : (double) low / total;
        return new KnowledgeGapSummary(total, low, ratio, distribution, since);
    }

    // 对时间窗内中低置信问题做规则聚类，按提问次数降序返回缺口主题。
    public List<KnowledgeGapCluster> cluster(int days) {
        List<ChatQueryLog> logs = chatQueryLogMapper.findLowConfidenceSince(since(days));
        return clusterLogs(logs);
    }

    // 贪心聚类：关键词 Jaccard 重叠达到阈值则归入已有组，否则开新组。
    List<KnowledgeGapCluster> clusterLogs(List<ChatQueryLog> logs) {
        AppProperties.KnowledgeGap config = properties.getKnowledgeGap();
        List<List<ChatQueryLog>> groups = new ArrayList<>();
        List<Set<String>> groupKeywords = new ArrayList<>();

        for (ChatQueryLog log : logs) {
            Set<String> keywords = extractKeywords(log.getQuestion(), config.getMaxKeywords());
            int matched = -1;
            for (int i = 0; i < groupKeywords.size(); i++) {
                if (jaccard(keywords, groupKeywords.get(i)) >= config.getClusterSimilarityThreshold()) {
                    matched = i;
                    break;
                }
            }
            if (matched >= 0) {
                groups.get(matched).add(log);
                groupKeywords.get(matched).addAll(keywords);
            } else {
                List<ChatQueryLog> group = new ArrayList<>();
                group.add(log);
                groups.add(group);
                groupKeywords.add(new LinkedHashSet<>(keywords));
            }
        }

        List<KnowledgeGapCluster> clusters = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            clusters.add(toCluster(groups.get(i), groupKeywords.get(i), config.getMaxSampleQuestions()));
        }
        clusters.sort((left, right) -> Integer.compare(right.questionCount(), left.questionCount()));
        return clusters;
    }

    // 单组聚合：代表问题取最早一条，样例取最近若干条。
    private KnowledgeGapCluster toCluster(List<ChatQueryLog> group, Set<String> keywords, int maxSamples) {
        double scoreSum = 0.0;
        int scoreCount = 0;
        for (ChatQueryLog log : group) {
            if (log.getTopVectorScore() != null) {
                scoreSum += log.getTopVectorScore();
                scoreCount++;
            }
        }
        double averageScore = scoreCount == 0 ? 0.0 : scoreSum / scoreCount;

        List<String> samples = new ArrayList<>();
        for (int i = group.size() - 1; i >= 0 && samples.size() < maxSamples; i--) {
            String question = group.get(i).getQuestion();
            if (!samples.contains(question)) {
                samples.add(question);
            }
        }
        return new KnowledgeGapCluster(
                group.get(0).getQuestion(),
                List.copyOf(keywords),
                group.size(),
                averageScore,
                samples,
                group.get(group.size() - 1).getCreatedAt()
        );
    }

    // HanLP 抽关键词，失败时退回整个问题文本，保证聚类不中断。
    Set<String> extractKeywords(String question, int maxKeywords) {
        if (question == null || question.isBlank()) {
            return Set.of();
        }
        try {
            List<String> extracted = HanLP.extractKeyword(question, maxKeywords);
            Set<String> keywords = new LinkedHashSet<>();
            for (String keyword : extracted) {
                if (keyword != null && !keyword.isBlank()) {
                    keywords.add(keyword.trim().toLowerCase());
                }
            }
            return keywords;
        } catch (RuntimeException ex) {
            return Set.of(question.trim().toLowerCase());
        }
    }

    // 关键词集合的 Jaccard 相似度。
    double jaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new LinkedHashSet<>(left);
        union.addAll(right);
        return (double) intersection.size() / union.size();
    }

    // 计算时间窗起点。
    private LocalDateTime since(int days) {
        int windowDays = days > 0 ? days : properties.getKnowledgeGap().getDefaultWindowDays();
        return LocalDateTime.now().minusDays(windowDays);
    }
}
