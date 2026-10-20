package com.xiaoyan.aiassistant.session;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// ChatSessionMapper 与 ChatMessageMapper 的 SQL 注解保护测试。
class ChatSessionMapperAnnotationTest {

    // 确保 Mapper 方法都有 SQL 注解，避免运行时才发现 MyBatis statement 不存在。
    @Test
    void sessionMapperMethodsShouldKeepSqlAnnotations() throws NoSuchMethodException {
        assertThat(ChatSessionMapper.class.getMethod("upsert", ChatSession.class).isAnnotationPresent(Insert.class)).isTrue();
        assertThat(ChatSessionMapper.class.getMethod("findByUserId", String.class).isAnnotationPresent(Select.class)).isTrue();
        assertThat(ChatSessionMapper.class.getMethod("delete", String.class, String.class).isAnnotationPresent(Delete.class)).isTrue();
    }

    // 消息 Mapper 同样保留注解。
    @Test
    void messageMapperMethodsShouldKeepSqlAnnotations() throws NoSuchMethodException {
        assertThat(ChatMessageMapper.class.getMethod("insert", ChatMessage.class).isAnnotationPresent(Insert.class)).isTrue();
        assertThat(ChatMessageMapper.class.getMethod("findBySession", String.class, String.class).isAnnotationPresent(Select.class)).isTrue();
        assertThat(ChatMessageMapper.class.getMethod("deleteBySession", String.class, String.class).isAnnotationPresent(Delete.class)).isTrue();
    }

    // 会话标题截断逻辑：超长提问收窄为 50 字标题。
    @Test
    void truncatesSessionTitle() {
        assertThat(ChatSessionService.title("短问题")).isEqualTo("短问题");
        assertThat(ChatSessionService.title("问".repeat(80))).hasSize(51).endsWith("…");
    }
}
