package com.njumarket.njumarket.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * WebSocket 事件监听器
 * 监听连接、断开、订阅等事件
 */
@Slf4j
@Component
public class WebSocketEventListener {
    
    // 用户ID -> Session ID 集合（支持多设备）
    private final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    /**
     * 连接建立事件
     * 注意：此时 STOMP 连接可能尚未完全建立，Principal 可能还未设置
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 从 Session 属性中获取 userId（在 HandshakeInterceptor 中设置）
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String userId = null;
        
        // 方式1：从 sessionAttributes 获取
        if (sessionAttributes != null) {
            userId = (String) sessionAttributes.get("userId");
        }
        
        // 方式2：从 Principal 获取
        Principal principal = headerAccessor.getUser();
        if (userId == null && principal != null) {
            userId = principal.getName();
        }
        
        if (userId != null) {
            userSessions.computeIfAbsent(userId, k -> new HashSet<>()).add(sessionId);
            log.debug("WebSocket connection established: userId={}, sessionId={}", userId, sessionId);
        } else {
            log.warn("WebSocket connection established but userId not found: sessionId={}", sessionId);
        }
    }
    
    /**
     * 连接断开事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String userId = sessionAttributes != null 
            ? (String) sessionAttributes.get("userId") 
            : null;
        
        if (userId != null) {
            userSessions.computeIfPresent(userId, (k, v) -> {
                v.remove(sessionId);
                if (v.isEmpty()) {
                    return null; // 移除空集合
                }
                return v;
            });
            
            log.info("WebSocket connection closed: userId={}, sessionId={}", userId, sessionId);
        } else {
            log.info("WebSocket connection closed: sessionId={}", sessionId);
        }
    }
    
    /**
     * Subscribe event
     * IMPORTANT: The userId from Principal.getName() must exactly match the receiverId used in convertAndSendToUser()
     */
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        // Get userId from Principal or session attributes
        String userId = null;
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            userId = principal.getName();
        }
        
        if (userId == null) {
            Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                userId = (String) sessionAttributes.get("userId");
            }
        }
        
        if (userId == null) {
            userId = "unknown";
        }
        
        log.info("WebSocket subscription: sessionId={}, userId={}, destination={}, Principal={}", 
                sessionId, userId, destination, principal != null ? principal.getName() : "NULL");
        
        // Critical check: Principal must be set for convertAndSendToUser() to work
        if (destination != null && destination.contains("/user/queue/message")) {
            if (principal == null) {
                log.error("CRITICAL: Principal is NULL in SUBSCRIBE for /user/queue/message! Messages to userId={} will NOT be delivered!", userId);
            } else {
                log.info("Subscription verified: userId={}, Principal.getName()={}, destination={}", 
                        userId, principal.getName(), destination);
            }
        }
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
    
    /**
     * 获取用户的所有 Session ID
     */
    public Set<String> getUserSessions(String userId) {
        return userSessions.getOrDefault(userId, new HashSet<>());
    }
}

