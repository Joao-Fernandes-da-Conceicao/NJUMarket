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
public interface MessageRepository extends JpaRepository<Message, String>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Message> {
    
    // 获取对话的所有消息（过滤双向删除）
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findByConversationId(@Param("conversationId") String conversationId, 
                                       Pageable pageable);
    
    // 获取对话的最新消息
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
           "ORDER BY m.createdAt DESC")
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
           "AND m.receiverId = :userId AND m.isRead = false " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true)")
    int countUnreadMessages(@Param("conversationId") String conversationId, 
                           @Param("userId") String userId);
    
    // 双向删除：设置发送方删除标记
    @Modifying
    @Query("UPDATE Message m SET m.deletedBySender = true WHERE m.messageId = :messageId")
    void markDeletedBySender(@Param("messageId") String messageId);
    
    // 双向删除：设置接收方删除标记
    @Modifying
    @Query("UPDATE Message m SET m.deletedByReceiver = true WHERE m.messageId = :messageId")
    void markDeletedByReceiver(@Param("messageId") String messageId);
    
    // 获取两个用户之间的消息历史
    @Query("SELECT m FROM Message m WHERE " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findMessagesBetweenUsers(@Param("userId1") String userId1, 
                                          @Param("userId2") String userId2,
                                          Pageable pageable);
    
    // 搜索消息内容
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.content LIKE %:keyword% " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> searchMessages(@Param("conversationId") String conversationId, 
                                @Param("keyword") String keyword,
                                Pageable pageable);
    
    // 获取特定时间之后的消息
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.createdAt > :afterTime " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findMessagesAfter(@Param("conversationId") String conversationId, 
                                   @Param("afterTime") LocalDateTime afterTime);
    
    // ✅ v1.3.x: 获取指定时间之前的消息（用于无限滚动加载历史消息）
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.createdAt < :beforeTime " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findMessagesBefore(@Param("conversationId") String conversationId,
                                     @Param("beforeTime") LocalDateTime beforeTime,
                                     Pageable pageable);
    
    // 获取用户发送的最后一条消息
    @Query("SELECT m FROM Message m WHERE m.senderId = :userId " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
           "ORDER BY m.createdAt DESC")
    List<Message> findLastMessageBySender(@Param("userId") String userId, Pageable pageable);
    
    // ✅ 管理端：获取对话的最后一条消息（不过滤双方都删除的，显示所有消息）
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "ORDER BY m.createdAt DESC")
    List<Message> findLastMessageForAdmin(@Param("conversationId") String conversationId, Pageable pageable);
    
    // ✅ 用户端：获取对话的最后一条消息（过滤用户删除的）
    // 如果用户是发送方，则不能是发送方已删除的；如果用户是接收方，则不能是接收方已删除的
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND NOT (" +
           "  (m.senderId = :userId AND m.deletedBySender = true) OR " +
           "  (m.receiverId = :userId AND m.deletedByReceiver = true)" +
           ") " +
           "ORDER BY m.createdAt DESC")
    List<Message> findLastMessageForUser(@Param("conversationId") String conversationId, 
                                         @Param("userId") String userId, 
                                         Pageable pageable);
    
    // ✅ 查询对话中接收方的未读消息（用于已读回执）
    // 查询条件：指定对话、指定接收方、未读、未被双方都删除
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.receiverId = :receiverId " +
           "AND m.isRead = false " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findUnreadMessagesByConversationAndReceiver(
            @Param("conversationId") String conversationId,
            @Param("receiverId") String receiverId);
}