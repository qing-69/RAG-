package com.xiaoyan.aiassistant.gap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 一轮问答的检索信号记录，用于知识缺口分析。
public class ChatQueryLog {
    private Long id;
    private String userId;
    private String sessionId;
    private String question;
    private String rewrittenQuery;
    private String subQueries;
    private String expandedKeywords;
    private Integer knowledgeCount;
    private Double topVectorScore;
    private Integer keywordHit;
    private String confidenceLevel;
    private Double confidenceScore;
    private String confidenceReason;
    private LocalDateTime createdAt;
}
