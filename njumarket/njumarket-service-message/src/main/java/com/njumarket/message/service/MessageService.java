package com.njumarket.message.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.MessageDTO;

/**
 * 消息服务接口
 */
public interface MessageService {
    
    // ========== 消息功能 ==========
    /**
     * 发送消息
     */
    Result sendMessage(MessageDTO messageDTO);
    
    /**
     * 获取会话列表
     */
    Result getConversations();
    
    /**
     * 获取聊天记录
     */
    Result getChatHistory(String userId, Integer page, Integer size);
    
    /**
     * 标记消息为已读
     */
    Result markAsRead(String messageId);
    
    /**
     * 批量标记为已读
     */
    Result batchMarkAsRead(String[] messageIds);
    
    /**
     * 删除消息
     */
    Result deleteMessage(String messageId);
    
    /**
     * 获取未读消息数量
     */
    Result getUnreadCount();
    
    /**
     * 搜索消息
     */
    Result searchMessages(String keyword, Integer page, Integer size);
    
    // ========== 联系方式相关 ==========
    /**
     * 请求查看联系方式
     */
    Result requestContact(String userId);
    
    /**
     * 授权查看联系方式
     */
    Result grantContact(String userId);
    
    // ========== 内部方法 ==========
    /**
     * 发送站内消息
     */
    Result sendInMessage(String senderId, String receiverId, String content);
    
    /**
     * 展示联系方式
     */
    Result revealContact(String requesterId, String ownerId);
    
    /**
     * 获取联系信息
     */
    Result getContactInfo(String userId, String targetUserId);
}

