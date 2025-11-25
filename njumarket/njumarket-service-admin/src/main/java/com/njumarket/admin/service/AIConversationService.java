package com.njumarket.admin.service;

import com.njumarket.njumarket.dto.Result;

import java.util.List;

/**
 * AI 聊天会话管理服务接口（管理端）
 */
public interface AIConversationService {
    
    /**
     * 获取会话列表（分页）
     */
    Result getConversationList(Integer page, Integer size, String userId, String status, String keyword, String sortProp, String sortOrder);
    
    /**
     * 获取会话详情
     */
    Result getConversationDetail(String conversationId);
    
    /**
     * 删除会话（软删除）
     */
    Result deleteConversation(String conversationId);
    
    /**
     * 批量删除会话
     */
    Result batchDeleteConversations(List<String> conversationIds);
    
    /**
     * 恢复会话（将状态从DELETED改为ACTIVE）
     */
    Result restoreConversation(String conversationId);
    
    /**
     * 获取会话统计信息
     */
    Result getConversationStatistics();
}

