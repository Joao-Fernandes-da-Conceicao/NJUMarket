package com.njumarket.message.service;

import com.njumarket.message.dto.SendMessageRequest;
import com.njumarket.njumarket.dto.Result;

public interface ConversationService {

    // 获取对话列表
    Result getConversations(String userId, int page, int size);

    // 获取对话详情（包含消息历史）
    Result getConversationDetail(String userId, String conversationId, int page, int size);

    // 获取指定时间之前的消息（用于无限滚动加载历史消息）
    Result getMessagesBefore(String userId, String conversationId, String beforeTime, int size);

    // 获取或创建对话（基于用户对，确保唯一性）
    Result getOrCreateConversation(String userId, String otherUserId);

    // 标记对话消息为已读
    Result markConversationAsRead(String userId, String conversationId);

    // 获取未读消息总数
    Result getUnreadCount(String userId);

    // 删除对话
    Result deleteConversation(String userId, String conversationId);

    // 获取与特定用户的对话
    Result getConversationWithUser(String userId, String otherUserId);
}
