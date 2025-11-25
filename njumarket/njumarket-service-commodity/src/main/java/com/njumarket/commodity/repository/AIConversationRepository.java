package com.njumarket.commodity.repository;

import com.njumarket.commodity.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI 聊天会话 Repository
 */
@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, String> {
    
    /**
     * 根据用户ID查询所有会话（按更新时间倒序）
     */
    List<AIConversation> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);
    
    /**
     * 根据用户ID查询所有活跃会话（按更新时间倒序）
     */
    default List<AIConversation> findActiveByUserId(String userId) {
        return findByUserIdAndStatusOrderByUpdatedAtDesc(userId, "ACTIVE");
    }
    
    /**
     * 根据会话ID和用户ID查询会话
     */
    Optional<AIConversation> findByConversationIdAndUserId(String conversationId, String userId);
    
    /**
     * 增加消息数量
     */
    @Modifying
    @Query("UPDATE AIConversation ac SET ac.messageCount = ac.messageCount + :increment, ac.updatedAt = CURRENT_TIMESTAMP WHERE ac.conversationId = :conversationId")
    void incrementMessageCount(@Param("conversationId") String conversationId, @Param("increment") int increment);
    
    /**
     * 更新会话标题
     */
    @Modifying
    @Query("UPDATE AIConversation ac SET ac.title = :title, ac.updatedAt = CURRENT_TIMESTAMP WHERE ac.conversationId = :conversationId")
    void updateTitle(@Param("conversationId") String conversationId, @Param("title") String title);
    
    /**
     * 更新会话状态
     */
    @Modifying
    @Query("UPDATE AIConversation ac SET ac.status = :status, ac.updatedAt = CURRENT_TIMESTAMP WHERE ac.conversationId = :conversationId")
    void updateStatus(@Param("conversationId") String conversationId, @Param("status") String status);
}

