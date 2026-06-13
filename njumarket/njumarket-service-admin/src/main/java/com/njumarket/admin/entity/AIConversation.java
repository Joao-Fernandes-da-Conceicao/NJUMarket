package com.njumarket.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 聊天会话实体类（管理端）
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
    private String title;
    
    @Column(name = "message_count")
    private Integer messageCount = 0;
    
    @Column(name = "status", length = 20)
    private String status = "ACTIVE";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 与 AI 服务一致：当前窗口内 user/assistant 条数（不含历史摘要行） */
    @Column(name = "window_message_count")
    private Integer windowMessageCount;

    @Column(name = "memory_summary", columnDefinition = "TEXT")
    private String memorySummary;
}

