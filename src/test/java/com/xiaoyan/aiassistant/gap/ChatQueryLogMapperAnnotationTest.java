package com.xiaoyan.aiassistant.gap;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// ChatQueryLogMapper 的 SQL 注解保护测试。
class ChatQueryLogMapperAnnotationTest {

    // 确保 Mapper 方法都有 SQL 注解，避免运行时才发现 MyBatis statement 不存在。
    @Test
    void chatQueryLogMapperMethodsShouldKeepSqlAnnotations() throws NoSuchMethodException {
        assertThat(ChatQueryLogMapper.class.getMethod("insert", ChatQueryLog.class).isAnnotationPresent(Insert.class)).isTrue();
        assertThat(ChatQueryLogMapper.class.getMethod("findLowConfidenceSince", LocalDateTime.class).isAnnotationPresent(Select.class)).isTrue();
        assertThat(ChatQueryLogMapper.class.getMethod("countSince", LocalDateTime.class).isAnnotationPresent(Select.class)).isTrue();
        assertThat(ChatQueryLogMapper.class.getMethod("countByLevelSince", LocalDateTime.class, String.class).isAnnotationPresent(Select.class)).isTrue();
    }
}
