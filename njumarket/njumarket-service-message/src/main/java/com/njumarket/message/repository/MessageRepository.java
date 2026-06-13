package com.njumarket.message.repository;

import com.njumarket.message.entity.Message;
import com.njumarket.message.repository.projection.MessageContentProjection;
import com.njumarket.message.repository.projection.MessageIdProjection;
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
    
    // ✅ 优化：只查询content和createdAt字段，避免回表
    // 用于更新对话的最后消息时，只需要这两个字段
    @Query("SELECT m.content as content, m.createdAt as createdAt FROM Message m WHERE m.conversationId = :conversationId " +
           "AND NOT (" +
           "  (m.senderId = :userId AND m.deletedBySender = true) OR " +
           "  (m.receiverId = :userId AND m.deletedByReceiver = true)" +
           ") " +
           "ORDER BY m.createdAt DESC")
    List<MessageContentProjection> findLastMessageContentForUser(@Param("conversationId") String conversationId, 
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
    
    // ✅ 优化：只查询messageId字段，避免回表
    // 用于已读回执时，只需要messageId列表
    @Query("SELECT m.messageId as messageId FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.receiverId = :receiverId " +
           "AND m.isRead = false " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
           "ORDER BY m.createdAt ASC")
    List<MessageIdProjection> findUnreadMessageIdsByConversationAndReceiver(
            @Param("conversationId") String conversationId,
            @Param("receiverId") String receiverId);

    // ✅ 增量轮询：查询用户消息中涉及的所有商品ID（去重）
    @Query("SELECT DISTINCT m.commodityId FROM Message m WHERE " +
           "(m.senderId = :userId OR m.receiverId = :userId) " +
           "AND m.commodityId IS NOT NULL " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true)")
    List<String> findDistinctCommodityIdsByUser(@Param("userId") String userId);

    // ✅ 增量轮询：查询用户消息中涉及的所有订单ID（去重）
    @Query("SELECT DISTINCT m.orderId FROM Message m WHERE " +
           "(m.senderId = :userId OR m.receiverId = :userId) " +
           "AND m.orderId IS NOT NULL " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true)")
    List<String> findDistinctOrderIdsByUser(@Param("userId") String userId);

    // ✅ 增量轮询（时间戳过滤）：查询指定时间之后消息中涉及的商品ID（去重）
    @Query("SELECT DISTINCT m.commodityId FROM Message m WHERE " +
           "(m.senderId = :userId OR m.receiverId = :userId) " +
           "AND m.commodityId IS NOT NULL " +
           "AND m.createdAt > :since " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true)")
    List<String> findDistinctCommodityIdsByUserAndSince(@Param("userId") String userId,
                                                         @Param("since") LocalDateTime since);

    // ✅ 增量轮询（时间戳过滤）：查询指定时间之后消息中涉及的订单ID（去重）
    @Query("SELECT DISTINCT m.orderId FROM Message m WHERE " +
           "(m.senderId = :userId OR m.receiverId = :userId) " +
           "AND m.orderId IS NOT NULL " +
           "AND m.createdAt > :since " +
           "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true)")
    List<String> findDistinctOrderIdsByUserAndSince(@Param("userId") String userId,
                                                     @Param("since") LocalDateTime since);
}

