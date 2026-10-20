package com.xiaoyan.aiassistant.controller;

import com.xiaoyan.aiassistant.session.ChatMessage;
import com.xiaoyan.aiassistant.session.ChatSession;
import com.xiaoyan.aiassistant.session.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 会话持久化查询与删除接口。
@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final ChatSessionService chatSessionService;

    // 查询用户的会话列表。
    @GetMapping
    public List<ChatSession> list(@RequestParam(defaultValue = "default") String userId) {
        return chatSessionService.listSessions(userId);
    }

    // 查询单个会话的全部消息。
    @GetMapping("/{sessionId}/messages")
    public List<ChatMessage> messages(@PathVariable String sessionId,
                                      @RequestParam(defaultValue = "default") String userId) {
        return chatSessionService.listMessages(userId, sessionId);
    }

    // 删除会话及其消息。
    @DeleteMapping("/{sessionId}")
    public void delete(@PathVariable String sessionId,
                       @RequestParam(defaultValue = "default") String userId) {
        chatSessionService.deleteSession(userId, sessionId);
    }
}
