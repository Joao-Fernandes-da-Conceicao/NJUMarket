package com.njumarket.commodity.service;

import com.njumarket.commodity.entity.AIConversation;

import java.util.List;
import java.util.Optional;

/**
 * AI 聊天会话服务接口
 */
public interface AIConversationService {
    
    /**
     * 创建或获取会话
     * 如果会话不存在，则创建；如果存在，则返回现有会话
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param title 会话标题（可选）
     * @return 会话对象
     */
    AIConversation createOrGetConversation(String conversationId, String userId, String title);
    
    /**
     * 更新会话标题
     * @param conversationId 会话ID
     * @param title 新标题
     */
    void updateTitle(String conversationId, String title);
    
    /**
     * 增加消息数量
     * @param conversationId 会话ID
     * @param increment 增量（通常为1或2，因为一次对话包含用户消息和助手回复）
     */
    void incrementMessageCount(String conversationId, int increment);
    
    /**
     * 更新会话（更新时间和消息数量）
     * @param conversationId 会话ID
     */
    void updateConversation(String conversationId);
    
    /**
     * 获取用户的所有会话列表
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 会话列表
     */
    List<AIConversation> getUserConversations(String userId, int limit);
    
    /**
     * 根据会话ID获取会话
     * @param conversationId 会话ID
     * @return 会话对象
     */
    Optional<AIConversation> getConversation(String conversationId);
    
    /**
     * 删除会话（软删除，设置状态为DELETED）
     * @param conversationId 会话ID
     * @param userId 用户ID（用于权限验证）
     */
    void deleteConversation(String conversationId, String userId);
}

