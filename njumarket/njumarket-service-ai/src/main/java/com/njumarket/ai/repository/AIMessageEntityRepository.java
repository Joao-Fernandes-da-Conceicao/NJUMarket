package com.njumarket.ai.repository;

import com.njumarket.ai.entity.AIMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AIMessageEntityRepository extends JpaRepository<AIMessageEntity, String> {

    List<AIMessageEntity> findTop100ByConversationIdOrderByCreatedAtDesc(String conversationId);

    List<AIMessageEntity> findTop500ByConversationIdOrderByCreatedAtDesc(String conversationId);

    List<AIMessageEntity> findTop30ByConversationIdOrderByCreatedAtDesc(String conversationId);

    Optional<AIMessageEntity> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);

    long countByConversationId(String conversationId);
}
