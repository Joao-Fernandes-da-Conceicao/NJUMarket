package com.njumarket.message.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.entity.Conversation;
import com.njumarket.njumarket.entity.Message;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.message.repository.ConversationRepository;
import com.njumarket.message.repository.MessageRepository;
import com.njumarket.message.service.WebSocketRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 内部API控制器
 * 用于微服务间调用，不对外暴露
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    
    private final WebSocketRetryService webSocketRetryService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    
    /**
     * WebSocket推送消息（内部接口，供其他服务调用）
     */
    @PostMapping("/websocket/push")
    public Result pushMessage(@RequestParam String receiverId,
                             @RequestParam String messageType,
                             @RequestBody Map<String, Object> messageData) {
        webSocketRetryService.pushWithRetry(receiverId, messageData, messageType);
        return Result.ok("推送成功");
    }

    /**
     * 获取会话详情（管理端内部接口）
     */
    @GetMapping("/conversation/{conversationId}")
    public Result getConversationById(@PathVariable String conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new BusinessException("会话不存在"));
        return Result.ok("查询成功", conversation);
    }

    /**
     * 完整更新会话（管理端内部接口）
     */
    @PutMapping("/conversation/{conversationId}/full")
    public Result updateConversationFull(@PathVariable String conversationId,
                                         @RequestBody Map<String, Object> payload) {
        Conversation c = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new BusinessException("会话不存在"));
        
        // 更新字段
        Object userId1 = payload.get("userId1");
        if (userId1 instanceof String) c.setUserId1(((String) userId1).trim());
        
        Object userId2 = payload.get("userId2");
        if (userId2 instanceof String) c.setUserId2(((String) userId2).trim());
        
        Object lastMessageContent = payload.get("lastMessageContent");
        if (lastMessageContent instanceof String) c.setLastMessageContent(((String) lastMessageContent).trim());
        
        Object status = payload.get("status");
        if (status instanceof String) {
            String st = ((String) status).trim();
            java.util.Set<String> allowedStatus = new java.util.HashSet<>(java.util.Arrays.asList("ACTIVE", "ARCHIVED", "DELETED"));
            if (allowedStatus.contains(st)) {
                c.setStatus(st);
            }
        }
        
        Object user1Visibility = payload.get("user1Visibility");
        if (user1Visibility instanceof Boolean) c.setUser1Visibility((Boolean) user1Visibility);
        
        Object user2Visibility = payload.get("user2Visibility");
        if (user2Visibility instanceof Boolean) c.setUser2Visibility((Boolean) user2Visibility);
        
        conversationRepository.save(c);
        return Result.ok("更新成功", c);
    }

    /**
     * 删除会话（管理端内部接口）
     */
    @DeleteMapping("/conversation/{conversationId}")
    public Result deleteConversation(@PathVariable String conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new BusinessException("会话不存在");
        }
        conversationRepository.deleteById(conversationId);
        return Result.ok("删除成功");
    }

    /**
     * 获取消息详情（管理端内部接口）
     */
    @GetMapping("/message/{messageId}")
    public Result getMessageById(@PathVariable String messageId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new BusinessException("消息不存在"));
        return Result.ok("查询成功", message);
    }

    /**
     * 完整更新消息（管理端内部接口）
     */
    @PutMapping("/message/{messageId}/full")
    public Result updateMessageFull(@PathVariable String messageId,
                                    @RequestBody Map<String, Object> payload) {
        Message m = messageRepository.findById(messageId)
            .orElseThrow(() -> new BusinessException("消息不存在"));
        
        // 更新字段
        Object content = payload.get("content");
        if (content instanceof String) m.setContent(((String) content).trim());
        
        Object deletedBySender = payload.get("deletedBySender");
        if (deletedBySender instanceof Boolean) m.setDeletedBySender((Boolean) deletedBySender);
        
        Object deletedByReceiver = payload.get("deletedByReceiver");
        if (deletedByReceiver instanceof Boolean) m.setDeletedByReceiver((Boolean) deletedByReceiver);
        
        Object isRead = payload.get("isRead");
        if (isRead instanceof Boolean) m.setIsRead((Boolean) isRead);
        
        messageRepository.save(m);
        return Result.ok("更新成功", m);
    }

    /**
     * 删除消息（管理端内部接口）
     */
    @DeleteMapping("/message/{messageId}")
    public Result deleteMessage(@PathVariable String messageId) {
        if (!messageRepository.existsById(messageId)) {
            throw new BusinessException("消息不存在");
        }
        messageRepository.deleteById(messageId);
        return Result.ok("删除成功");
    }
    
    /**
     * 获取会话列表（管理端内部接口）
     */
    @GetMapping("/conversations")
    public Result listConversations(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) String keyword) {
        org.springframework.data.jpa.domain.Specification<Conversation> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (org.springframework.util.StringUtils.hasText(keyword)) {
                String kw = keyword.trim();
                predicates.add(cb.or(
                    cb.like(root.get("userId1"), "%" + kw + "%"),
                    cb.like(root.get("userId2"), "%" + kw + "%"),
                    cb.like(root.get("lastMessageContent"), "%" + kw + "%")
                ));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
            Math.max(0, page - 1), size, 
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastMessageTime"));
        org.springframework.data.domain.Page<Conversation> p = conversationRepository.findAll(spec, pageable);
        
        return Result.ok("查询成功", p);
    }
    
    /**
     * 获取消息列表（管理端内部接口）
     */
    @GetMapping("/messages")
    public Result listMessages(@RequestParam String conversationId,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size) {
        org.springframework.data.jpa.domain.Specification<Message> spec = (root, query, cb) -> {
            return cb.equal(root.get("conversationId"), conversationId);
        };
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
            Math.max(0, page - 1), size,
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<Message> p = messageRepository.findAll(spec, pageable);
        
        return Result.ok("查询成功", p);
    }
}

