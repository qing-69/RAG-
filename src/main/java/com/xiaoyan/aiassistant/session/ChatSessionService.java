package com.xiaoyan.aiassistant.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.aiassistant.chat.ConfidenceReport;
import com.xiaoyan.aiassistant.chat.SourceRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// 会话与消息的持久化服务，落库失败只告警，不打断问答主链路。
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {
    private static final int TITLE_MAX_CHARS = 50;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;

    // 保存一轮问答：刷新会话时间、写入用户问题和助手回答。
    public void saveTurn(String userId, String sessionId, String question, String answer,
                         ConfidenceReport confidence, List<SourceRef> sources) {
        try {
            LocalDateTime now = LocalDateTime.now();
            chatSessionMapper.upsert(new ChatSession(
                    null, userId, sessionId, title(question), now, now
            ));
            chatMessageMapper.insert(new ChatMessage(
                    null, userId, sessionId, "USER", question, null, null, now
            ));
            chatMessageMapper.insert(new ChatMessage(
                    null, userId, sessionId, "ASSISTANT", answer,
                    confidence == null ? null : confidence.level(),
                    sourcesJson(sources),
                    now
            ));
        } catch (RuntimeException ex) {
            log.warn("Save chat session turn failed: {}", ex.getMessage());
        }
    }

    // 查询用户的会话列表。
    public List<ChatSession> listSessions(String userId) {
        return chatSessionMapper.findByUserId(userId);
    }

    // 查询单个会话的全部消息。
    public List<ChatMessage> listMessages(String userId, String sessionId) {
        return chatMessageMapper.findBySession(userId, sessionId);
    }

    // 删除会话及其消息。
    public void deleteSession(String userId, String sessionId) {
        chatMessageMapper.deleteBySession(userId, sessionId);
        chatSessionMapper.delete(userId, sessionId);
    }

    // 会话标题取首轮提问，超长截断。
    static String title(String question) {
        String normalized = question == null ? "" : question.trim();
        return normalized.length() <= TITLE_MAX_CHARS
                ? normalized
                : normalized.substring(0, TITLE_MAX_CHARS) + "…";
    }

    // 来源列表序列化为单行 JSON 存库，失败返回 null 不阻断保存。
    private String sourcesJson(List<SourceRef> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception ex) {
            log.warn("Serialize sources failed: {}", ex.getMessage());
            return null;
        }
    }
}
