package com.xiaoyan.aiassistant.chat;

import com.xiaoyan.aiassistant.memory.ConversationMemory;
import com.xiaoyan.aiassistant.retrieval.RetrievalCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// PromptBuilder 的引用溯源与约束文案测试。
class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    // 知识库片段带 [n] 编号，长期记忆用（记忆n），避免编号混淆。
    @Test
    void numbersKnowledgeAndMemoryDifferently() {
        String prompt = promptBuilder.build(
                "问题",
                new QueryRewriteResult("改写问题", List.of(), List.of()),
                new ConversationMemory("", List.of()),
                List.of(candidate("记忆内容")),
                List.of(candidate("知识内容")),
                new ConfidenceReport(ConfidenceReport.LEVEL_HIGH, 0.9, "高", 1, 0.9)
        );
        assertThat(prompt).contains("[1] ");
        assertThat(prompt).contains("（记忆1） ");
        assertThat(prompt).doesNotContain("[2] ");
    }

    // 引用溯源约束要求标注 [1]、[2] 来源编号。
    @Test
    void appendsCitationGuide() {
        String prompt = promptBuilder.build(
                "问题",
                new QueryRewriteResult("改写问题", List.of(), List.of()),
                new ConversationMemory("", List.of()),
                List.of(),
                List.of(candidate("知识内容")),
                new ConfidenceReport(ConfidenceReport.LEVEL_HIGH, 0.9, "高", 1, 0.9)
        );
        assertThat(prompt).contains("标注来源编号");
    }

    // 非高置信才追加反幻觉强约束。
    @Test
    void appendsGuardOnlyWhenNotHigh() {
        QueryRewriteResult rewrite = new QueryRewriteResult("改写问题", List.of(), List.of());
        String highPrompt = promptBuilder.build("问题", rewrite, new ConversationMemory("", List.of()), List.of(), List.of(),
                new ConfidenceReport(ConfidenceReport.LEVEL_HIGH, 0.9, "高", 1, 0.9));
        String lowPrompt = promptBuilder.build("问题", rewrite, new ConversationMemory("", List.of()), List.of(), List.of(),
                new ConfidenceReport(ConfidenceReport.LEVEL_LOW, 0.3, "低", 1, 0.3));
        assertThat(highPrompt).doesNotContain("置信度偏低");
        assertThat(lowPrompt).contains("置信度偏低");
    }

    private RetrievalCandidate candidate(String content) {
        return new RetrievalCandidate(
                "v-1", 10L, 20L, "标题", "doc.txt", content, 0.9, 0, 0.01
        );
    }
}
