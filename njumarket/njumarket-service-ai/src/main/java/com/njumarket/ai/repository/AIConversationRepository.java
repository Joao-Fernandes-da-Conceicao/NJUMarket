package com.njumarket.ai.repository;

import com.njumarket.ai.entity.AIConversation;
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
    
    List<AIConversation> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);
    
    default List<AIConversation> findActiveByUserId(String userId) {
        return findByUserIdAndStatusOrderByUpdatedAtDesc(userId, "ACTIVE");
    }
    
    Optional<AIConversation> findByConversationIdAndUserId(String conversationId, String userId);
    
    @Modifying
    @Query("UPDATE AIConversation ac SET ac.messageCount = ac.messageCount + :increment, ac.updatedAt = CURRENT_TIMESTAMP WHERE ac.conversationId = :conversationId")
    void incrementMessageCount(@Param("conversationId") String conversationId, @Param("increment") int increment);
    
    @Modifying
    @Query("UPDATE AIConversation ac SET ac.title = :title, ac.updatedAt = CURRENT_TIMESTAMP WHERE ac.conversationId = :conversationId")
    void updateTitle(@Param("conversationId") String conversationId, @Param("title") String title);
    
    @Modifying
    @Query("UPDATE AIConversation ac SET ac.status = :status, ac.updatedAt = CURRENT_TIMESTAMP WHERE ac.conversationId = :conversationId")
    void updateStatus(@Param("conversationId") String conversationId, @Param("status") String status);
}

