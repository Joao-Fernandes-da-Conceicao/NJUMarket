package com.njumarket.notification.controller;

import com.njumarket.notification.service.WebSocketRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket ACK确认控制器（Notification Service）
 * 接收前端发送的消息确认（ACK），用于标记消息已成功接收
 * 
 * 消息格式：
 * {
 *   "messageId": "消息ID",
 *   "messageType": "ORDER_CHANGE" | "COMMODITY_CHANGE" | "UNREAD_COUNT_UPDATE" 等
 * }
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketAckController {
    
    private final WebSocketRetryService webSocketRetryService;
    
    /**
     * 接收消息确认（ACK）
     * 前端通过 /app/ack 发送ACK消息
     * 
     * @param ackData ACK数据，包含messageId和messageType
     * @param principal 当前用户（由WebSocket认证提供）
     */
    @MessageMapping("/ack")
    public void handleAck(@Payload Map<String, Object> ackData, Principal principal) {
        try {
            // ✅ 详细日志：记录ACK接收的完整信息
            log.info("📥 [ACK接收] 收到ACK请求: ackData={}, principal={}", ackData, principal != null ? principal.getName() : "null");
            
            String userId = principal != null ? principal.getName() : null;
            String messageId = (String) ackData.get("messageId");
            String messageType = (String) ackData.get("messageType");
            
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("❌ [ACK验证失败] 收到ACK但用户未认证: ackData={}", ackData);
                return;
            }
            
            if (messageId == null || messageId.trim().isEmpty()) {
                log.warn("❌ [ACK验证失败] 收到ACK但messageId为空: userId={}, ackData={}", userId, ackData);
                return;
            }
            
            log.info("✅ [ACK验证通过] 收到ACK确认: userId={}, messageId={}, messageType={}, timestamp={}", 
                userId, messageId, messageType, java.time.LocalDateTime.now());
            
            // 处理ACK，从重试队列中移除消息
            webSocketRetryService.handleAck(userId, messageId, messageType);
            
        } catch (Exception e) {
            log.error("❌ [ACK处理异常] 处理ACK失败: ackData={}, error={}", ackData, e.getMessage(), e);
        }
    }
}

