package com.njumarket.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_conversations", schema = "nju_market")
public class AIConversationEntity {

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

    /** 当前窗口内 user/assistant 消息条数（不含【历史摘要】） */
    @Column(name = "window_message_count", nullable = false)
    private Integer windowMessageCount = 0;

    /** 【历史摘要】正文（不含前缀） */
    @Column(name = "memory_summary", columnDefinition = "TEXT")
    private String memorySummary;
}
