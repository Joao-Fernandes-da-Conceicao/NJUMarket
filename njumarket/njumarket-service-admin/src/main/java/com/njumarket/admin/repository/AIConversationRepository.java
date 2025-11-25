package com.njumarket.admin.repository;

import com.njumarket.admin.entity.AIConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


/**
 * AI 聊天会话 Repository（管理端）
 */
@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, String> {
    
    /**
     * 根据用户ID查询会话列表
     */
    Page<AIConversation> findByUserId(String userId, Pageable pageable);
    
    /**
     * 根据状态查询会话列表
     */
    Page<AIConversation> findByStatus(String status, Pageable pageable);
    
    /**
     * 根据用户ID和状态查询会话列表
     */
    Page<AIConversation> findByUserIdAndStatus(String userId, String status, Pageable pageable);
    
    /**
     * 根据标题模糊查询
     */
    @Query("SELECT ac FROM AIConversation ac WHERE ac.title LIKE %:keyword%")
    Page<AIConversation> findByTitleContaining(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 统计会话数量
     */
    long countByStatus(String status);
    
    /**
     * 统计用户会话数量
     */
    long countByUserId(String userId);
}

