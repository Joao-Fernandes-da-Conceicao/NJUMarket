package com.njumarket.ai.service;

import com.njumarket.ai.entity.AIConversation;

import java.util.List;
import java.util.Optional;

/**
 * AI 聊天会话服务接口
 */
public interface AIConversationService {
    
    AIConversation createOrGetConversation(String conversationId, String userId, String title);
    
    void updateTitle(String conversationId, String title);
    
    void incrementMessageCount(String conversationId, int increment);
    
    void updateConversation(String conversationId);
    
    List<AIConversation> getUserConversations(String userId, int limit);
    
    Optional<AIConversation> getConversation(String conversationId);
    
    void deleteConversation(String conversationId, String userId);
}

