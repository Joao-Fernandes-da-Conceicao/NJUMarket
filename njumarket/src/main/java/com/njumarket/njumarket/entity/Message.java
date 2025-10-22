package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息实体类
 * 用于用户之间的站内消息通信
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    @Column(name = "message_id", length = 50)
    private String messageId;
    
    @Column(name = "sender_id", length = 50, nullable = false)
    private String senderId;
    
    @Column(name = "receiver_id", length = 50, nullable = false)
    private String receiverId;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @CreationTimestamp
    @Column(name = "send_time", nullable = false)
    private LocalDateTime sendTime;
    
    @Column(name = "message_type", length = 20, nullable = false)
    private String messageType; // TEXT, IMAGE, SYSTEM, NOTIFICATION
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    // 多对一关系：消息属于某个发送者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    private User sender;
    
    // 多对一关系：消息属于某个接收者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", insertable = false, updatable = false)
    private User receiver;
    
    /**
     * 发送消息
     * @return 发送是否成功
     */
    public Boolean sendMessage() {
        this.sendTime = LocalDateTime.now();
        this.isRead = false;
        return true;
    }
    
    /**
     * 获取消息历史记录
     * @return 消息列表
     */
    public static List<Message> getMessageHistory(String senderId, String receiverId) {
        // 业务逻辑：查询两个用户之间的消息历史
        return null;
    }
}
