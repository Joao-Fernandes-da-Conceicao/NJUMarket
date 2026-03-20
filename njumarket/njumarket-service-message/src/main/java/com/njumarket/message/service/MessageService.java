package com.njumarket.message.service;

import com.njumarket.message.dto.SendMessageRequest;
import com.njumarket.njumarket.dto.Result;

public interface MessageService {

    // 发送消息
    Result sendMessage(String userId, SendMessageRequest request);

    // 删除消息
    Result deleteMessage(String userId, String messageId);

    // 搜索消息
    Result searchMessages(String userId, String conversationId, String keyword, int page, int size);

    // 屏蔽用户
    Result blockUser(String userId, String blockedUserId, String reason);

    // 取消屏蔽用户
    Result unblockUser(String userId, String blockedUserId);

    // 检查是否被屏蔽
    Result isBlocked(String userId, String otherUserId);
}
