package com.xiaoyan.aiassistant.feedback;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// ChatFeedbackMapper 的 SQL 注解保护测试。
class ChatFeedbackMapperAnnotationTest {

    // 确保 Mapper 方法都有 SQL 注解，避免运行时才发现 MyBatis statement 不存在。
    @Test
    void feedbackMapperMethodsShouldKeepSqlAnnotations() throws NoSuchMethodException {
        assertThat(ChatFeedbackMapper.class.getMethod("insert", ChatFeedback.class).isAnnotationPresent(Insert.class)).isTrue();
        assertThat(ChatFeedbackMapper.class.getMethod("countByRatingSince", LocalDateTime.class, String.class).isAnnotationPresent(Select.class)).isTrue();
        assertThat(ChatFeedbackMapper.class.getMethod("findSince", LocalDateTime.class).isAnnotationPresent(Select.class)).isTrue();
    }
}
