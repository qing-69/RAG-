package com.xiaoyan.aiassistant.chat;

// 置信度评估结果，携带等级、得分和给用户看的原因。
public record ConfidenceReport(
        String level,
        double score,
        String reason,
        int knowledgeCount,
        double topVectorScore
) {
    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_LOW = "LOW";

    public ConfidenceReport {
        level = level == null ? LEVEL_LOW : level;
        reason = reason == null ? "" : reason.trim();
    }

    // 是否可以按正常口径回答。
    public boolean isHigh() {
        return LEVEL_HIGH.equals(level);
    }
}
