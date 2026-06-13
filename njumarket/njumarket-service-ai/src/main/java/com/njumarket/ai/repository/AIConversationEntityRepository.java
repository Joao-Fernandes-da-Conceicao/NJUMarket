package com.njumarket.ai.repository;

import com.njumarket.ai.entity.AIConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AIConversationEntityRepository extends JpaRepository<AIConversationEntity, String> {

    @Query("SELECT c FROM AIConversationEntity c WHERE c.userId = :userId AND c.status <> 'DELETED' ORDER BY c.updatedAt DESC")
    List<AIConversationEntity> findActiveByUserIdOrderByUpdatedAtDesc(@Param("userId") String userId);
}
