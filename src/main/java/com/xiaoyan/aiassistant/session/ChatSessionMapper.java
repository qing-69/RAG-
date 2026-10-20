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
// 会话元数据的 MyBatis 数据访问接口。
public interface ChatSessionMapper {

    // 新建会话；同一用户重复会话只刷新时间，保留首次提问作为标题。
    @Insert("""
            insert into chat_session(user_id, session_id, title, created_at, updated_at)
            values(#{userId}, #{sessionId}, #{title}, #{createdAt}, #{updatedAt})
            on duplicate key update updated_at = values(updated_at)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void upsert(ChatSession session);

    // 按用户查询会话列表，最近活跃的排前面。
    @Select("""
            select id, user_id, session_id, title, created_at, updated_at
            from chat_session
            where user_id = #{userId}
            order by updated_at desc
            """)
    List<ChatSession> findByUserId(@Param("userId") String userId);

    // 删除单个会话元数据。
    @Delete("""
            delete from chat_session
            where user_id = #{userId} and session_id = #{sessionId}
            """)
    void delete(@Param("userId") String userId, @Param("sessionId") String sessionId);
}
