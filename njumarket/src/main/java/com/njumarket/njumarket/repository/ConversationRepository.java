package com.njumarket.njumarket.repository;

import com.njumarket.njumarket.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

//import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    
    // 查找买家和卖家之间的对话
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.buyerId = :buyerId AND c.sellerId = :sellerId) OR " +
           "(c.buyerId = :sellerId AND c.sellerId = :buyerId)")
    Optional<Conversation> findByBuyerIdAndSellerId(@Param("buyerId") String buyerId, 
                                                    @Param("sellerId") String sellerId);
    
    // 查找关于特定商品的对话
    @Query("SELECT c FROM Conversation c WHERE " +
           "c.commodityId = :commodityId AND " +
           "((c.buyerId = :buyerId AND c.sellerId = :sellerId) OR " +
           "(c.buyerId = :sellerId AND c.sellerId = :buyerId))")
    Optional<Conversation> findByCommodityAndUsers(@Param("commodityId") String commodityId,
                                                   @Param("buyerId") String buyerId,
                                                   @Param("sellerId") String sellerId);
    
    // 获取用户的所有对话（作为买家或卖家）
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.buyerId = :userId OR c.sellerId = :userId) AND " +
           "c.status = :status " +
           "ORDER BY c.lastMessageTime DESC")
    Page<Conversation> findByUserIdAndStatus(@Param("userId") String userId, 
                                            @Param("status") String status,
                                            Pageable pageable);
    
    // 获取用户的所有对话
    @Query("SELECT c FROM Conversation c WHERE " +
           "c.buyerId = :userId OR c.sellerId = :userId " +
           "ORDER BY c.lastMessageTime DESC")
    Page<Conversation> findByUserId(@Param("userId") String userId, Pageable pageable);
    
    // 获取用户未读消息总数
    @Query("SELECT SUM(CASE WHEN c.buyerId = :userId THEN c.buyerUnreadCount " +
           "ELSE c.sellerUnreadCount END) " +
           "FROM Conversation c " +
           "WHERE (c.buyerId = :userId OR c.sellerId = :userId) AND c.status = 'ACTIVE'")
    Integer getTotalUnreadCount(@Param("userId") String userId);
    
    // 标记对话为已读
    @Modifying
    @Query("UPDATE Conversation c SET " +
           "c.buyerUnreadCount = CASE WHEN c.buyerId = :userId THEN 0 ELSE c.buyerUnreadCount END, " +
           "c.sellerUnreadCount = CASE WHEN c.sellerId = :userId THEN 0 ELSE c.sellerUnreadCount END " +
           "WHERE c.conversationId = :conversationId")
    void markAsReadForUser(@Param("conversationId") String conversationId, 
                           @Param("userId") String userId);
    
    // 增加未读消息数
    @Modifying
    @Query("UPDATE Conversation c SET " +
           "c.buyerUnreadCount = CASE WHEN c.buyerId = :receiverId THEN c.buyerUnreadCount + 1 ELSE c.buyerUnreadCount END, " +
           "c.sellerUnreadCount = CASE WHEN c.sellerId = :receiverId THEN c.sellerUnreadCount + 1 ELSE c.sellerUnreadCount END " +
           "WHERE c.conversationId = :conversationId")
    void incrementUnreadCount(@Param("conversationId") String conversationId, 
                             @Param("receiverId") String receiverId);
    
    // 更新最后一条消息
    @Modifying
    @Query("UPDATE Conversation c SET " +
           "c.lastMessageContent = :content, " +
           "c.lastMessageTime = CURRENT_TIMESTAMP " +
           "WHERE c.conversationId = :conversationId")
    void updateLastMessage(@Param("conversationId") String conversationId, 
                          @Param("content") String content);
    
    // 删除对话（软删除）
    @Modifying
    @Query("UPDATE Conversation c SET c.status = 'DELETED' " +
           "WHERE c.conversationId = :conversationId")
    void softDelete(@Param("conversationId") String conversationId);
    
    // 归档对话
    @Modifying
    @Query("UPDATE Conversation c SET c.status = 'ARCHIVED' " +
           "WHERE c.conversationId = :conversationId")
    void archive(@Param("conversationId") String conversationId);
}
