package com.xiaoyan.aiassistant.gap;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@SuppressWarnings("SqlResolve")
// 问答检索信号的 MyBatis 数据访问接口。
public interface ChatQueryLogMapper {

    // 记录一轮问答的检索信号和置信度。
    @Insert("""
            insert into chat_query_log(user_id, session_id, question, rewritten_query, sub_queries, expanded_keywords,
            knowledge_count, top_vector_score, keyword_hit, confidence_level, confidence_score, confidence_reason, created_at)
            values(#{userId}, #{sessionId}, #{question}, #{rewrittenQuery}, #{subQueries}, #{expandedKeywords},
            #{knowledgeCount}, #{topVectorScore}, #{keywordHit}, #{confidenceLevel}, #{confidenceScore}, #{confidenceReason}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatQueryLog queryLog);

    // 查询时间窗内中低置信问题，作为缺口聚类的原料。
    @Select("""
            select id, user_id, session_id, question, rewritten_query, sub_queries, expanded_keywords,
            knowledge_count, top_vector_score, keyword_hit, confidence_level, confidence_score, confidence_reason, created_at
            from chat_query_log
            where created_at >= #{since} and confidence_level in ('LOW', 'MEDIUM')
            order by created_at
            """)
    List<ChatQueryLog> findLowConfidenceSince(@Param("since") LocalDateTime since);

    // 统计时间窗内全部提问数。
    @Select("""
            select count(*)
            from chat_query_log
            where created_at >= #{since}
            """)
    long countSince(@Param("since") LocalDateTime since);

    // 统计时间窗内指定置信度等级的提问数。
    @Select("""
            select count(*)
            from chat_query_log
            where created_at >= #{since} and confidence_level = #{level}
            """)
    long countByLevelSince(@Param("since") LocalDateTime since, @Param("level") String level);
}
