package com.njumarket.njumarket.controller.user;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.MessageDTO;
import com.njumarket.njumarket.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户消息控制器
 */
@RestController
@RequestMapping("/api/user/message")
@RequiredArgsConstructor
public class UserMessageController {

    private final MessageService messageService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result sendMessage(@RequestBody MessageDTO messageDTO) {
        return messageService.sendMessage(messageDTO);
    }

    /**
     * 获取消息列表（会话列表）
     */
    @GetMapping("/conversations")
    public Result getConversations() {
        return messageService.getConversations();
    }

    /**
     * 获取与某用户的聊天记录
     */
    @GetMapping("/chat/{userId}")
    public Result getChatHistory(@PathVariable String userId,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "20") Integer size) {
        return messageService.getChatHistory(userId, page, size);
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/{messageId}/read")
    public Result markAsRead(@PathVariable String messageId) {
        return messageService.markAsRead(messageId);
    }

    /**
     * 批量标记消息为已读
     */
    @PostMapping("/batch-read")
    public Result batchMarkAsRead(@RequestBody String[] messageIds) {
        return messageService.batchMarkAsRead(messageIds);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    public Result deleteMessage(@PathVariable String messageId) {
        return messageService.deleteMessage(messageId);
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result getUnreadCount() {
        return messageService.getUnreadCount();
    }

    /**
     * 搜索消息
     */
    @GetMapping("/search")
    public Result searchMessages(@RequestParam String keyword,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size) {
        return messageService.searchMessages(keyword, page, size);
    }

    /**
     * 请求查看联系方式
     */
    @PostMapping("/request-contact/{userId}")
    public Result requestContact(@PathVariable String userId) {
        return messageService.requestContact(userId);
    }

    /**
     * 授权查看联系方式
     */
    @PostMapping("/grant-contact/{userId}")
    public Result grantContact(@PathVariable String userId) {
        return messageService.grantContact(userId);
    }
}
