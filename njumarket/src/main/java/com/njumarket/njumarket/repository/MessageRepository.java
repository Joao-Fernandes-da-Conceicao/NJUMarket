package com.njumarket.njumarket.repository;

import com.njumarket.njumarket.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    
    // 获取对话的所有消息
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<Message> findByConversationId(@Param("conversationId") String conversationId, 
                                       Pageable pageable);
    
    // 获取对话的最新消息
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.isDeleted = false ORDER BY m.createdAt DESC")
    List<Message> findLatestMessages(@Param("conversationId") String conversationId, 
                                     Pageable pageable);
    
    // 标记消息为已读
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true, m.readTime = :readTime " +
           "WHERE m.conversationId = :conversationId AND m.receiverId = :userId " +
           "AND m.isRead = false")
    int markMessagesAsRead(@Param("conversationId") String conversationId, 
                          @Param("userId") String userId,
                          @Param("readTime") LocalDateTime readTime);
    
    // 获取未读消息数量
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.receiverId = :userId AND m.isRead = false AND m.isDeleted = false")
    int countUnreadMessages(@Param("conversationId") String conversationId, 
                           @Param("userId") String userId);
    
    // 软删除消息
    @Modifying
    @Query("UPDATE Message m SET m.isDeleted = true WHERE m.messageId = :messageId")
    void softDelete(@Param("messageId") String messageId);
    
    // 获取两个用户之间的消息历史
    @Query("SELECT m FROM Message m WHERE " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<Message> findMessagesBetweenUsers(@Param("userId1") String userId1, 
                                          @Param("userId2") String userId2,
                                          Pageable pageable);
    
    // 搜索消息内容
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.content LIKE %:keyword% AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<Message> searchMessages(@Param("conversationId") String conversationId, 
                                @Param("keyword") String keyword,
                                Pageable pageable);
    
    // 获取特定时间之后的消息
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.createdAt > :afterTime AND m.isDeleted = false " +
           "ORDER BY m.createdAt ASC")
    List<Message> findMessagesAfter(@Param("conversationId") String conversationId, 
                                    @Param("afterTime") LocalDateTime afterTime);
    
    // 获取用户发送的最后一条消息
    @Query("SELECT m FROM Message m WHERE m.senderId = :userId " +
           "AND m.isDeleted = false ORDER BY m.createdAt DESC")
    List<Message> findLastMessageBySender(@Param("userId") String userId, Pageable pageable);
}