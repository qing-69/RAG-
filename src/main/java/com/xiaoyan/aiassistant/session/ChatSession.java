package com.xiaoyan.aiassistant.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 会话持久化实体，一个会话对应多条问答消息。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
    private Long id;
    private String userId;
    private String sessionId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
