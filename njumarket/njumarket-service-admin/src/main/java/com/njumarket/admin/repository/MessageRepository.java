package com.njumarket.admin.repository;

import com.njumarket.admin.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息数据访问层（管理端）
 * 管理端直接访问数据库，不需要通过FeignClient
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, String>, JpaSpecificationExecutor<Message> {
    
    /**
     * 根据会话ID查询消息列表
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.createdAt DESC")
    List<Message> findByConversationId(@Param("conversationId") String conversationId);
    
    /**
     * ✅ 用户端：获取对话的最后一条消息（过滤用户删除的）
     * 如果用户是发送方，则不能是发送方已删除的；如果用户是接收方，则不能是接收方已删除的
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND NOT (" +
           "  (m.senderId = :userId AND m.deletedBySender = true) OR " +
           "  (m.receiverId = :userId AND m.deletedByReceiver = true)" +
           ") " +
           "ORDER BY m.createdAt DESC")
    List<Message> findLastMessageForUser(@Param("conversationId") String conversationId, 
                                         @Param("userId") String userId, 
                                         Pageable pageable);
}

