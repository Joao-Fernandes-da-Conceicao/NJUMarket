package com.njumarket.notification.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

/**
 * WebSocket事件监听器（推送服务）
 * 用于检查用户在线状态
 */
@Slf4j
@Component
@ConditionalOnBean(SimpUserRegistry.class)
public class WebSocketEventListener {
    
    private final SimpUserRegistry simpUserRegistry;
    
    public WebSocketEventListener(SimpUserRegistry simpUserRegistry) {
        this.simpUserRegistry = simpUserRegistry;
    }
    
    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return true表示用户在线，false表示用户离线
     */
    public boolean isUserOnline(String userId) {
        try {
            SimpUser user = simpUserRegistry.getUser(userId);
            boolean online = user != null;
            
            if (online && user != null) {
                log.debug("用户在线（推送服务）: userId={}", userId);
            } else {
                log.debug("用户离线（推送服务）: userId={}", userId);
            }
            
            return online;
        } catch (Exception e) {
            log.error("检查用户在线状态失败（推送服务）: userId={}, error={}, errorType={}", 
                userId, e.getMessage(), e.getClass().getName(), e);
            return false;
        }
    }
}

