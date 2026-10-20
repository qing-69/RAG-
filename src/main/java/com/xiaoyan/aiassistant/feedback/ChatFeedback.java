package com.xiaoyan.aiassistant.feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 问答反馈实体，rating 取值 GOOD / BAD。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatFeedback {
    private Long id;
    private String userId;
    private String sessionId;
    private String question;
    private String rating;
    private String comment;
    private LocalDateTime createdAt;
}
