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
@Table(name = "ai_messages", schema = "nju_market")
public class AIMessageEntity {

    @Id
    @Column(name = "message_id", length = 64)
    private String messageId;

    @Column(name = "conversation_id", length = 50, nullable = false)
    private String conversationId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "recommended_commodity_ids")
    private String recommendedCommodityIds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
