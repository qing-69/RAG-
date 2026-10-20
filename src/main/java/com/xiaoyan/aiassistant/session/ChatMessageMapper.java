package com.xiaoyan.aiassistant.session;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@SuppressWarnings("SqlResolve")
// 会话消息的 MyBatis 数据访问接口。
public interface ChatMessageMapper {

    // 写入一条用户或助手消息。
    @Insert("""
            insert into chat_message(user_id, session_id, role, content, confidence_level, sources_json, created_at)
            values(#{userId}, #{sessionId}, #{role}, #{content}, #{confidenceLevel}, #{sourcesJson}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatMessage message);

    // 按会话查询消息，按时间正序恢复对话上下文。
    @Select("""
            select id, user_id, session_id, role, content, confidence_level, sources_json, created_at
            from chat_message
            where user_id = #{userId} and session_id = #{sessionId}
            order by created_at, id
            """)
    List<ChatMessage> findBySession(@Param("userId") String userId, @Param("sessionId") String sessionId);

    // 删除会话下全部消息。
    @Delete("""
            delete from chat_message
            where user_id = #{userId} and session_id = #{sessionId}
            """)
    void deleteBySession(@Param("userId") String userId, @Param("sessionId") String sessionId);
}
