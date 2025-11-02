package com.njumarket.njumarket.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.CloseStatus;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket连接监控服务
 * 监控连接状态，记录连接/断开事件，用于分析和告警
 * 
 * 注意：本服务主要用于监控和日志记录，不参与重连逻辑
 * 重连机制由前端实现（更快速响应，减少后端负担）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMonitorService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 连接建立时的处理
     * 记录连接事件，可选发送连接确认消息给前端
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 获取userId
        String userId = getUserIdFromEvent(headerAccessor);
        
        if (userId != null) {
            log.info("WebSocket connected: userId={}, sessionId={}", userId, sessionId);
            
            // 可选：发送连接确认消息（帮助前端验证连接）
            try {
                Map<String, Object> confirmation = Map.of(
                    "type", "CONNECTION_CONFIRMED",
                    "timestamp", LocalDateTime.now().toString()
                );
                messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/connection",
                    confirmation
                );
                log.debug("Connection confirmation sent to userId={}", userId);
            } catch (Exception e) {
                log.warn("Failed to send connection confirmation to userId={}: {}", userId, e.getMessage());
            }
        } else {
            log.warn("WebSocket connected but userId not found: sessionId={}", sessionId);
        }
    }
    
    /**
     * 连接断开时的处理
     * 记录断开事件，分析断开原因（正常/异常）
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        CloseStatus closeStatus = event.getCloseStatus();
        
        // 获取userId
        String userId = getUserIdFromEvent(headerAccessor);
        
        if (userId != null) {
            log.info("WebSocket disconnected: userId={}, sessionId={}, closeStatus={}", 
                userId, sessionId, closeStatus);
            
            // 记录异常断开（用于分析和告警）
            if (closeStatus != null && !closeStatus.equals(CloseStatus.NORMAL)) {
                log.warn("Abnormal WebSocket disconnect: userId={}, sessionId={}, status={}, reason={}", 
                    userId, sessionId, closeStatus.getCode(), closeStatus.getReason());
            }
        } else {
            log.info("WebSocket disconnected: sessionId={}, closeStatus={}", sessionId, closeStatus);
        }
    }
    
    /**
     * 定期检查连接状态（可选）
     * 用于监控连接健康度，生产环境可以用于告警
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void checkConnectionHealth() {
        // Spring WebSocket会自动清理失效的连接，这里主要用于日志记录
        log.debug("WebSocket connection health check performed");
        // 可以在这里添加连接数统计、异常连接检测等逻辑
    }
    
    /**
     * 从事件中提取userId
     */
    private String getUserIdFromEvent(StompHeaderAccessor headerAccessor) {
        // 方式1：从Principal获取
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            return principal.getName();
        }
        
        // 方式2：从sessionAttributes获取
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            return (String) sessionAttributes.get("userId");
        }
        
        return null;
    }
}

