package com.xiaoyan.aiassistant.chat;

import com.xiaoyan.aiassistant.retrieval.RetrievalCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// SourceRef 的来源映射测试。
class SourceRefTest {

    // 空检索结果映射为空来源列表。
    @Test
    void mapsEmptyKnowledgeToEmptyRefs() {
        assertThat(SourceRef.fromCandidates(List.of())).isEmpty();
        assertThat(SourceRef.fromCandidates(null)).isEmpty();
    }

    // 编号从 1 开始且与候选顺序一致，答案里的 [n] 能对上。
    @Test
    void mapsCandidatesWithSequentialIndexes() {
        List<SourceRef> refs = SourceRef.fromCandidates(List.of(
                candidate("doc-a.txt", "第一段内容"),
                candidate("doc-b.txt", "第二段内容")
        ));
        assertThat(refs).hasSize(2);
        assertThat(refs.get(0).index()).isEqualTo(1);
        assertThat(refs.get(0).source()).isEqualTo("doc-a.txt");
        assertThat(refs.get(1).index()).isEqualTo(2);
        assertThat(refs.get(1).source()).isEqualTo("doc-b.txt");
    }

    // 超长片段截断到 160 字并追加省略号。
    @Test
    void truncatesLongSnippet() {
        String longContent = "长".repeat(300);
        List<SourceRef> refs = SourceRef.fromCandidates(List.of(candidate("doc.txt", longContent)));
        assertThat(refs.get(0).snippet()).hasSize(161).endsWith("…");
    }

    // vectorScore > 0 时 scoreType 为 vector，score 直接取 vectorScore。
    @Test
    void usesVectorScoreWhenAvailable() {
        List<SourceRef> refs = SourceRef.fromCandidates(List.of(candidate("doc.txt", "内容")));
        assertThat(refs.get(0).scoreType()).isEqualTo("vector");
        assertThat(refs.get(0).score()).isEqualTo(0.9);
    }

    // vectorScore = 0 时降级为 keyword 类型，score 归一化到 (0,1)。
    @Test
    void fallsBackToNormalizedKeywordScoreWhenVectorScoreIsZero() {
        RetrievalCandidate bm25Only = new RetrievalCandidate(
                "v-2", 10L, 20L, "章节", "doc.txt", "内容", 0, 8.0, 0.01
        );
        List<SourceRef> refs = SourceRef.fromCandidates(List.of(bm25Only));
        assertThat(refs.get(0).scoreType()).isEqualTo("keyword");
        assertThat(refs.get(0).score()).isGreaterThan(0).isLessThan(1);
    }

    // 无任何命中时 score 为 0，scoreType 为 keyword。
    @Test
    void zeroScoresProduceKeywordTypeWithZeroScore() {
        RetrievalCandidate empty = new RetrievalCandidate(
                "v-3", 10L, 20L, "章节", "doc.txt", "内容", 0, 0, 0
        );
        List<SourceRef> refs = SourceRef.fromCandidates(List.of(empty));
        assertThat(refs.get(0).scoreType()).isEqualTo("keyword");
        assertThat(refs.get(0).score()).isEqualTo(0);
    }

    private RetrievalCandidate candidate(String source, String content) {
        return new RetrievalCandidate(
                "v-1", 10L, 20L, "章节", source, content, 0.9, 0, 0.01
        );
    }
}
