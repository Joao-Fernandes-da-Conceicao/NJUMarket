package com.njumarket.ai.service;

import com.njumarket.ai.storage.AIConversationStorage;

import java.util.List;
import java.util.Optional;

/**
 * AI 聊天会话服务接口（基于 Redis + Milvus，无关系库）
 */
public interface AIConversationService {

    AIConversationStorage.ConvMeta createOrGetConversation(String conversationId, String userId, String title);

    void updateTitle(String conversationId, String title);

    void incrementMessageCount(String conversationId, int increment);

    List<AIConversationStorage.ConvMeta> getUserConversations(String userId, int limit);

    void deleteConversation(String conversationId, String userId);

    Optional<AIConversationStorage.MessageRecord> getLatestMessage(String conversationId);

    List<AIConversationStorage.MessageRecord> getMessages(String conversationId, String userId);
}
