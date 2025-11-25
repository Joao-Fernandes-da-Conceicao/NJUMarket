package com.njumarket.commodity.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 聊天会话实体类
 * 用于统一管理 AI 聊天会话，避免 conversation_id 悬空
 */
@Entity
@Table(name = "ai_conversations", schema = "nju_market")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIConversation {
    
    @Id
    @Column(name = "conversation_id", length = 50)
    private String conversationId;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    @Column(name = "title", length = 200)
    private String title; // 会话标题（第一条用户消息的前50个字符）
    
    @Column(name = "message_count")
    private Integer messageCount = 0; // 消息数量（冗余字段，可通过查询计算）
    
    @Column(name = "status", length = 20)
    private String status = "ACTIVE"; // 状态：ACTIVE（活跃）、DELETED（已删除）
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

