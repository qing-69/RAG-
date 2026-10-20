package com.xiaoyan.aiassistant.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 会话消息实体，记录用户提问和助手回答。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long id;
    private String userId;
    private String sessionId;
    private String role;
    private String content;
    private String confidenceLevel;
    private String sourcesJson;
    private LocalDateTime createdAt;
}
