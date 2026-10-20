package com.xiaoyan.aiassistant.chat;

import com.xiaoyan.aiassistant.retrieval.RetrievalCandidate;

import java.util.ArrayList;
import java.util.List;

// 回答引用来源，index 与 Prompt 中知识库片段的 [1] [2] 编号一一对应。
// scoreType 标识分数来源：vector=向量余弦相似度，keyword=仅 BM25 关键词命中。
public record SourceRef(
        int index,
        Long documentId,
        Long chunkId,
        String title,
        String source,
        String snippet,
        double score,
        String scoreType
) {

    private static final int SNIPPET_MAX_CHARS = 160;

    // 把检索候选按顺序映射为引用来源列表。
    public static List<SourceRef> fromCandidates(List<RetrievalCandidate> knowledge) {
        List<SourceRef> refs = new ArrayList<>();
        if (knowledge == null || knowledge.isEmpty()) {
            return refs;
        }
        for (int i = 0; i < knowledge.size(); i++) {
            RetrievalCandidate candidate = knowledge.get(i);
            boolean hasVector = candidate.getVectorScore() > 0;
            refs.add(new SourceRef(
                    i + 1,
                    candidate.getDocumentId(),
                    candidate.getChunkId(),
                    candidate.getTitle(),
                    candidate.getSource(),
                    snippet(candidate.getContent()),
                    hasVector ? candidate.getVectorScore() : normalizeKeywordScore(candidate.getKeywordScore()),
                    hasVector ? "vector" : "keyword"
            ));
        }
        return refs;
    }

    // 将 BM25 无界分数映射到 (0, 1) 区间，供前端展示；无命中时返回 0。
    private static double normalizeKeywordScore(double keywordScore) {
        if (keywordScore <= 0) {
            return 0;
        }
        return keywordScore / (keywordScore + 5.0);
    }

    // 截断片段内容，控制 sources 帧体积并便于前端展示。
    private static String snippet(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replace('\n', ' ').trim();
        return normalized.length() <= SNIPPET_MAX_CHARS
                ? normalized
                : normalized.substring(0, SNIPPET_MAX_CHARS) + "…";
    }
}
