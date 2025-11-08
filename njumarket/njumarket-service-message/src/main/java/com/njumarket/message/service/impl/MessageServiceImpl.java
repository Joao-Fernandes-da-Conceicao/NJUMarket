package com.njumarket.message.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.MessageDTO;
import com.njumarket.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    // ========== 消息功能 ==========
    @Override
    public Result sendMessage(MessageDTO messageDTO) {
        log.info("发送消息 - messageDTO: {}", messageDTO);
        return Result.ok("发送消息成功");
    }

    @Override
    public Result getConversations() {
        log.info("获取会话列表");
        return Result.ok("获取会话列表成功");
    }

    @Override
    public Result getChatHistory(String userId, Integer page, Integer size) {
        log.info("获取聊天记录 - userId: {}, page: {}, size: {}", userId, page, size);
        return Result.ok("获取聊天记录成功");
    }

    @Override
    public Result markAsRead(String messageId) {
        log.info("标记消息为已读 - messageId: {}", messageId);
        return Result.ok("标记消息为已读成功");
    }

    @Override
    public Result batchMarkAsRead(String[] messageIds) {
        log.info("批量标记为已读 - messageIds: {}", (Object) messageIds);
        return Result.ok("批量标记为已读成功");
    }

    @Override
    public Result deleteMessage(String messageId) {
        log.info("删除消息 - messageId: {}", messageId);
        return Result.ok("删除消息成功");
    }

    @Override
    public Result getUnreadCount() {
        log.info("获取未读消息数量");
        return Result.ok("获取未读消息数量成功");
    }

    @Override
    public Result searchMessages(String keyword, Integer page, Integer size) {
        log.info("搜索消息 - keyword: {}, page: {}, size: {}", keyword, page, size);
        return Result.ok("搜索消息成功");
    }

    // ========== 联系方式相关 ==========
    @Override
    public Result requestContact(String userId) {
        log.info("请求查看联系方式 - userId: {}", userId);
        return Result.ok("请求查看联系方式成功");
    }

    @Override
    public Result grantContact(String userId) {
        log.info("授权查看联系方式 - userId: {}", userId);
        return Result.ok("授权查看联系方式成功");
    }

    // ========== 内部方法 ==========
    @Override
    public Result sendInMessage(String senderId, String receiverId, String content) {
        log.info("发送站内消息 - senderId: {}, receiverId: {}, content: {}", senderId, receiverId, content);
        return Result.ok("发送站内消息成功");
    }

    @Override
    public Result revealContact(String requesterId, String ownerId) {
        log.info("展示联系方式 - requesterId: {}, ownerId: {}", requesterId, ownerId);
        return Result.ok("展示联系方式成功");
    }

    @Override
    public Result getContactInfo(String userId, String targetUserId) {
        log.info("获取联系信息 - userId: {}, targetUserId: {}", userId, targetUserId);
        return Result.ok("获取联系信息成功");
    }
}