package com.njumarket.njumarket.repository;

import com.njumarket.njumarket.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息数据访问层
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    
    /**
     * 根据发送者ID查找消息
     */
    List<Message> findBySenderId(String senderId);
    
    /**
     * 根据接收者ID查找消息
     */
    List<Message> findByReceiverId(String receiverId);
    
    /**
     * 获取两个用户之间的聊天记录
     */
    @Query("SELECT m FROM Message m WHERE (m.senderId = ?1 AND m.receiverId = ?2) OR (m.senderId = ?2 AND m.receiverId = ?1) ORDER BY m.sendTime ASC")
    List<Message> findChatHistory(String userId1, String userId2);
    
    /**
     * 获取用户的未读消息
     */
    @Query("SELECT m FROM Message m WHERE m.receiverId = ?1 AND m.isRead = false ORDER BY m.sendTime DESC")
    List<Message> findUnreadMessages(String receiverId);
    
    /**
     * 统计用户未读消息数量
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = ?1 AND m.isRead = false")
    Long countUnreadMessages(String receiverId);
}
