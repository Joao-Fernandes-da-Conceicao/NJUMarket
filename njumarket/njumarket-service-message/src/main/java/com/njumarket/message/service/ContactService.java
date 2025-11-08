package com.njumarket.message.service;

//import com.njumarket.njumarket.dto.ConversationDTO;
//import com.njumarket.njumarket.dto.MessageDTO;
import com.njumarket.njumarket.dto.SendMessageRequest;
import com.njumarket.njumarket.dto.Result;
//import org.springframework.data.domain.Page;

public interface ContactService {
    
    // 发送消息
    Result sendMessage(String userId, SendMessageRequest request);
    
    // 获取对话列表
    Result getConversations(String userId, int page, int size);
    
    // 获取对话详情（包含消息历史）
    Result getConversationDetail(String userId, String conversationId, int page, int size);
    
    // ✅ v1.3.x: 获取指定时间之前的消息（用于无限滚动加载历史消息）
    Result getMessagesBefore(String userId, String conversationId, String beforeTime, int size);
    
    // 获取或创建对话（基于用户对，确保唯一性，不再依赖商品/订单）
    Result getOrCreateConversation(String userId, String otherUserId);
    
    // 标记对话消息为已读
    Result markConversationAsRead(String userId, String conversationId);
    
    // 获取未读消息总数
    Result getUnreadCount(String userId);
    
    // 删除对话
    Result deleteConversation(String userId, String conversationId);
    
    // 删除消息
    Result deleteMessage(String userId, String messageId);
    
    // 搜索消息
    Result searchMessages(String userId, String conversationId, String keyword, int page, int size);
    
    // 获取与特定用户的对话
    Result getConversationWithUser(String userId, String otherUserId);
    
    // 屏蔽用户
    Result blockUser(String userId, String blockedUserId, String reason);
    
    // 取消屏蔽用户
    Result unblockUser(String userId, String blockedUserId);
    
    // 检查是否被屏蔽
    Result isBlocked(String userId, String otherUserId);
}

