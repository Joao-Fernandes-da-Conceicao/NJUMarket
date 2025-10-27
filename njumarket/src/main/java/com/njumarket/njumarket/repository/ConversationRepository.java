package com.njumarket.njumarket.repository;

import com.njumarket.njumarket.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    
    /**
     * 查找两个用户之间的对话（基于标准化的用户对）
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "((c.userId1 = :userId1 AND c.userId2 = :userId2) OR " +
           "(c.userId1 = :userId2 AND c.userId2 = :userId1)) AND " +
           "c.status = :status")
    Optional<Conversation> findByUserPair(@Param("userId1") String userId1,
                                         @Param("userId2") String userId2,
                                         @Param("status") String status);
    
    /**
     * 查找两个用户之间的活跃对话（默认状态为ACTIVE）
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "((c.userId1 = :userId1 AND c.userId2 = :userId2) OR " +
           "(c.userId1 = :userId2 AND c.userId2 = :userId1)) AND " +
           "c.status = 'ACTIVE'")
    Optional<Conversation> findByUserPairActive(@Param("userId1") String userId1,
                                                @Param("userId2") String userId2);
    
    // 兼容方法：查找买家和卖家之间的对话（向后兼容）
    @Query("SELECT c FROM Conversation c WHERE " +
           "((c.userId1 = :buyerId AND c.userId2 = :sellerId) OR " +
           "(c.userId1 = :sellerId AND c.userId2 = :buyerId))")
    Optional<Conversation> findByBuyerIdAndSellerId(@Param("buyerId") String buyerId, 
                                                    @Param("sellerId") String sellerId);
    
    /**
     * 获取用户的所有对话（作为userId1或userId2）
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.userId1 = :userId OR c.userId2 = :userId) AND " +
           "c.status = :status " +
           "ORDER BY c.lastMessageTime DESC")
    Page<Conversation> findByUserIdAndStatus(@Param("userId") String userId, 
                                            @Param("status") String status,
                                            Pageable pageable);
    
    /**
     * 获取用户的所有对话
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "c.userId1 = :userId OR c.userId2 = :userId " +
           "ORDER BY c.lastMessageTime DESC")
    Page<Conversation> findByUserId(@Param("userId") String userId, Pageable pageable);
    
    /**
     * 获取用户对话列表（按最后消息时间排序）
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.userId1 = :userId OR c.userId2 = :userId) AND " +
           "c.status = 'ACTIVE' " +
           "ORDER BY c.lastMessageTime DESC")
    List<Conversation> findByUserIdOrderByLastMessageTime(@Param("userId") String userId);
    
    /**
     * 获取用户未读消息总数
     */
    @Query("SELECT SUM(CASE WHEN c.userId1 = :userId THEN c.user1Count " +
           "ELSE c.user2Count END) " +
           "FROM Conversation c " +
           "WHERE (c.userId1 = :userId OR c.userId2 = :userId) AND c.status = 'ACTIVE'")
    Integer getTotalUnreadCount(@Param("userId") String userId);
    
    /**
     * 标记对话为已读
     */
    @Modifying
    @Query("UPDATE Conversation c SET " +
           "c.user1Count = CASE WHEN c.userId1 = :userId THEN 0 ELSE c.user1Count END, " +
           "c.user2Count = CASE WHEN c.userId2 = :userId THEN 0 ELSE c.user2Count END " +
           "WHERE c.conversationId = :conversationId")
    void markAsReadForUser(@Param("conversationId") String conversationId, 
                           @Param("userId") String userId);
    
    /**
     * 增加未读消息数
     */
    @Modifying
    @Query("UPDATE Conversation c SET " +
           "c.user1Count = CASE WHEN c.userId1 = :receiverId THEN c.user1Count + 1 ELSE c.user1Count END, " +
           "c.user2Count = CASE WHEN c.userId2 = :receiverId THEN c.user2Count + 1 ELSE c.user2Count END " +
           "WHERE c.conversationId = :conversationId")
    void incrementUnreadCount(@Param("conversationId") String conversationId, 
                             @Param("receiverId") String receiverId);
    
    /**
     * 更新最后一条消息
     */
    @Modifying
    @Query("UPDATE Conversation c SET " +
           "c.lastMessageContent = :content, " +
           "c.lastMessageTime = CURRENT_TIMESTAMP " +
           "WHERE c.conversationId = :conversationId")
    void updateLastMessage(@Param("conversationId") String conversationId, 
                          @Param("content") String content);
    
    /**
     * 删除对话（软删除）
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.status = 'DELETED' " +
           "WHERE c.conversationId = :conversationId")
    void softDelete(@Param("conversationId") String conversationId);
    
    /**
     * 归档对话
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.status = 'ARCHIVED' " +
           "WHERE c.conversationId = :conversationId")
    void archive(@Param("conversationId") String conversationId);
}
