package com.njumarket.admin.repository;

import com.njumarket.admin.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 会话数据访问层（管理端）
 * 管理端直接访问数据库，不需要通过FeignClient
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String>, JpaSpecificationExecutor<Conversation> {
}

