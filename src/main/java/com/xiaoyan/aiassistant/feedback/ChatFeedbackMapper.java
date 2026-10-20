package com.xiaoyan.aiassistant.feedback;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@SuppressWarnings("SqlResolve")
// 问答反馈的 MyBatis 数据访问接口。
public interface ChatFeedbackMapper {

    // 写入一条用户反馈。
    @Insert("""
            insert into chat_feedback(user_id, session_id, question, rating, comment, created_at)
            values(#{userId}, #{sessionId}, #{question}, #{rating}, #{comment}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatFeedback feedback);

    // 统计时间窗内指定评分的反馈数。
    @Select("""
            select count(*)
            from chat_feedback
            where created_at >= #{since} and rating = #{rating}
            """)
    long countByRatingSince(@Param("since") LocalDateTime since, @Param("rating") String rating);

    // 查询时间窗内全部反馈，按时间倒序。
    @Select("""
            select id, user_id, session_id, question, rating, comment, created_at
            from chat_feedback
            where created_at >= #{since}
            order by created_at desc
            """)
    List<ChatFeedback> findSince(@Param("since") LocalDateTime since);
}
