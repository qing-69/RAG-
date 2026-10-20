package com.xiaoyan.aiassistant.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.aiassistant.gap.ChatQueryLog;
import com.xiaoyan.aiassistant.gap.ChatQueryLogMapper;
import com.xiaoyan.aiassistant.memory.ConversationMemory;
import com.xiaoyan.aiassistant.memory.ShortTermMemoryService;
import com.xiaoyan.aiassistant.retrieval.HybridRetrievalService;
import com.xiaoyan.aiassistant.retrieval.RetrievalCandidate;
import com.xiaoyan.aiassistant.session.ChatSessionService;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 负责在线问答主链路编排。
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private static final String DEFAULT_USER_ID = "default";
    private static final String META_PREFIX = "@meta@";
    private static final String SOURCES_PREFIX = "@sources@";

    private final ShortTermMemoryService shortTermMemoryService;
    private final QueryRewriteService queryRewriteService;
    private final HybridRetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final StreamingChatModel streamingChatModel;
    private final ConfidenceService confidenceService;
    private final ChatQueryLogMapper chatQueryLogMapper;
    private final ChatSessionService chatSessionService;
    private final ObjectMapper objectMapper;

    // 在线问答主流程：记忆读取、Query 重写、RAG 检索、置信度评估、流式生成、记忆更新。
    public Flux<String> stream(ChatRequest request) {
        String userId = normalizeUserId(request.userId());
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : "default";
        String memorySessionId = userId + ":" + sessionId;
        String message = request.message();
        return Flux.create(sink -> {
            ConversationMemory memory = shortTermMemoryService.get(memorySessionId);
            QueryRewriteResult rewrite = queryRewriteService.rewrite(message, memory);
            List<String> semanticQueries = rewrite.semanticQueries(message);
            List<RetrievalCandidate> longMemories = retrievalService.retrieveLongMemory(userId, semanticQueries);
            List<RetrievalCandidate> knowledge = retrievalService.retrieveKnowledge(semanticQueries, rewrite.keywordText(message));
            ConfidenceReport confidence = confidenceService.assess(knowledge);
            String prompt = promptBuilder.build(message, rewrite, memory, longMemories, knowledge, confidence);
            List<SourceRef> sources = SourceRef.fromCandidates(knowledge);
            // 正文前先推送置信度元数据帧和引用来源帧，前端据此渲染徽章与来源卡片。
            sink.next(metaFrame(confidence));
            sink.next(sourcesFrame(sources));
            StringBuilder answer = new StringBuilder();
            streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
                // 收到流式片段后立即推给前端。
                @Override
                public void onPartialResponse(String partialResponse) {
                    answer.append(partialResponse);
                    sink.next(partialResponse);
                }

                // 完整回答结束后写入短期记忆、会话消息和问答日志。
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    shortTermMemoryService.append(memorySessionId, message, answer.toString());
                    chatSessionService.saveTurn(userId, sessionId, message, answer.toString(), confidence, sources);
                    recordQueryLog(userId, sessionId, message, rewrite, knowledge, confidence);
                    sink.complete();
                }

                // 模型调用异常时交给响应流向前端暴露。
                @Override
                public void onError(Throwable error) {
                    sink.error(error);
                }
            });
        });
    }

    // 置信度元数据帧，@meta@ 哨兵前缀 + 单行 JSON，不进入回答正文。
    private String metaFrame(ConfidenceReport confidence) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("level", confidence.level());
            meta.put("score", confidence.score());
            meta.put("reason", confidence.reason());
            meta.put("knowledgeCount", confidence.knowledgeCount());
            return META_PREFIX + objectMapper.writeValueAsString(meta);
        } catch (Exception ex) {
            log.warn("Build confidence meta frame failed, fallback to default: {}", ex.getMessage());
            return META_PREFIX + "{\"level\":\"" + confidence.level() + "\"}";
        }
    }

    // 引用来源帧，@sources@ 哨兵前缀 + 单行 JSON 数组，index 对应答案里的 [n] 标注。
    private String sourcesFrame(List<SourceRef> sources) {
        try {
            return SOURCES_PREFIX + objectMapper.writeValueAsString(sources);
        } catch (Exception ex) {
            log.warn("Build sources frame failed, fallback to empty: {}", ex.getMessage());
            return SOURCES_PREFIX + "[]";
        }
    }

    // 落问答检索日志供缺口分析，失败只告警不影响问答。
    private void recordQueryLog(String userId, String sessionId, String message,
                                QueryRewriteResult rewrite, List<RetrievalCandidate> knowledge,
                                ConfidenceReport confidence) {
        try {
            double topVectorScore = knowledge.stream()
                    .mapToDouble(RetrievalCandidate::getVectorScore)
                    .max()
                    .orElse(0.0);
            int keywordHit = knowledge.stream().anyMatch(candidate -> candidate.getKeywordScore() > 0) ? 1 : 0;
            ChatQueryLog queryLog = new ChatQueryLog(
                    null,
                    userId,
                    sessionId,
                    message,
                    rewrite.rewrittenQuery(),
                    String.join(";", rewrite.subQueries()),
                    String.join(",", rewrite.expandedKeywords()),
                    confidence.knowledgeCount(),
                    topVectorScore,
                    keywordHit,
                    confidence.level(),
                    confidence.score(),
                    confidence.reason(),
                    LocalDateTime.now()
            );
            chatQueryLogMapper.insert(queryLog);
        } catch (RuntimeException ex) {
            log.warn("Record chat query log failed: {}", ex.getMessage());
        }
    }

    // 空用户统一归入 default，避免 Redis key 和长期记忆过滤出现空值。
    private String normalizeUserId(String userId) {
        return StringUtils.hasText(userId) ? userId.trim() : DEFAULT_USER_ID;
    }
}
