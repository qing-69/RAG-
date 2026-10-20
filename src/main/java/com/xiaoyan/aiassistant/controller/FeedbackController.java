package com.xiaoyan.aiassistant.controller;

import com.xiaoyan.aiassistant.feedback.ChatFeedback;
import com.xiaoyan.aiassistant.feedback.ChatFeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 问答反馈提交与统计接口。
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    private final ChatFeedbackMapper chatFeedbackMapper;

    // 提交一条反馈，rating 约定为 GOOD 或 BAD。
    @PostMapping
    public ChatFeedback create(@RequestBody ChatFeedback feedback) {
        // question 为 NOT NULL，缺省时兜底为空串，避免直接 500。
        if (feedback.getQuestion() == null) {
            feedback.setQuestion("");
        }
        feedback.setCreatedAt(LocalDateTime.now());
        chatFeedbackMapper.insert(feedback);
        return feedback;
    }

    // 统计时间窗内好坏反馈数量。
    @GetMapping("/stats")
    public Map<String, Long> stats(@RequestParam(defaultValue = "30") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("good", chatFeedbackMapper.countByRatingSince(since, "GOOD"));
        stats.put("bad", chatFeedbackMapper.countByRatingSince(since, "BAD"));
        stats.put("days", (long) days);
        return stats;
    }

    // 查询时间窗内全部反馈记录。
    @GetMapping
    public List<ChatFeedback> list(@RequestParam(defaultValue = "30") int days) {
        return chatFeedbackMapper.findSince(LocalDateTime.now().minusDays(days));
    }
}
