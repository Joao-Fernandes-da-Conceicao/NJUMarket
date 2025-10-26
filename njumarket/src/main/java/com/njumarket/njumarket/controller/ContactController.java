package com.njumarket.njumarket.controller;

//import com.njumarket.njumarket.dto.ConversationDTO;
//import com.njumarket.njumarket.dto.MessageDTO;
import com.njumarket.njumarket.dto.SendMessageRequest;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    
    @Autowired
    private ContactService contactService;
    
    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result sendMessage(@RequestAttribute("userId") String userId,
                             @RequestBody SendMessageRequest request) {
        return contactService.sendMessage(userId, request);
    }
    
    /**
     * 获取对话列表
     */
    @GetMapping("/conversations")
    public Result getConversations(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return contactService.getConversations(userId, page - 1, size);
    }
    
    /**
     * 获取对话详情（包含消息历史）
     */
    @GetMapping("/conversations/{conversationId}")
    public Result getConversationDetail(
            @RequestAttribute("userId") String userId,
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return contactService.getConversationDetail(userId, conversationId, page - 1, size);
    }
    
    /**
     * 创建或获取对话
     */
    @PostMapping("/conversations/create")
    public Result createConversation(
            @RequestAttribute("userId") String userId,
            @RequestParam String otherUserId,
            @RequestParam(required = false) String commodityId,
            @RequestParam(required = false) String orderId) {
        return contactService.getOrCreateConversation(userId, otherUserId, commodityId, orderId);
    }
    
    /**
     * 标记对话为已读
     */
    @PostMapping("/conversations/{conversationId}/read")
    public Result markAsRead(@RequestAttribute("userId") String userId,
                            @PathVariable String conversationId) {
        return contactService.markConversationAsRead(userId, conversationId);
    }
    
    /**
     * 获取未读消息总数
     */
    @GetMapping("/unread-count")
    public Result getUnreadCount(@RequestAttribute("userId") String userId) {
        return contactService.getUnreadCount(userId);
    }
    
    /**
     * 删除对话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Result deleteConversation(@RequestAttribute("userId") String userId,
                                    @PathVariable String conversationId) {
        return contactService.deleteConversation(userId, conversationId);
    }
    
    /**
     * 删除消息
     */
    @DeleteMapping("/messages/{messageId}")
    public Result deleteMessage(@RequestAttribute("userId") String userId,
                               @PathVariable String messageId) {
        return contactService.deleteMessage(userId, messageId);
    }
    
    /**
     * 搜索消息
     */
    @GetMapping("/conversations/{conversationId}/search")
    public Result searchMessages(
            @RequestAttribute("userId") String userId,
            @PathVariable String conversationId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return contactService.searchMessages(userId, conversationId, keyword, page - 1, size);
    }
    
    /**
     * 获取与特定用户的对话
     */
    @GetMapping("/conversations/with/{otherUserId}")
    public Result getConversationWithUser(
            @RequestAttribute("userId") String userId,
            @PathVariable String otherUserId) {
        return contactService.getConversationWithUser(userId, otherUserId);
    }
}
