package com.njumarket.njumarket.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

/**
 * WebSocket事件监听器
 * 用于检查用户在线状态
 * 放在common模块中，供所有服务使用（如商品更新、消息推送等）
 * 
 * 注意：只在有WebSocket支持的服务中加载（需要SimpUserRegistry Bean）
 * Gateway等响应式服务不会加载此组件
 */
@Slf4j
@Component
@ConditionalOnBean(SimpUserRegistry.class)  // 只在有SimpUserRegistry Bean的服务中加载
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
            
            // ✅ 增强日志：记录所有在线用户（用于调试）
            if (online && user != null) {
                log.debug("用户在线: userId={}, sessionCount={}", userId, 
                    user.getSessions() != null ? user.getSessions().size() : 0);
            } else {
                log.debug("用户离线: userId={}", userId);
                
                // ✅ 调试：列出所有在线用户（用于排查问题）
                int totalUsers = simpUserRegistry.getUserCount();
                log.debug("当前在线用户总数: {}, 查询的userId={}", totalUsers, userId);
                
                // 列出所有在线用户的ID（仅用于调试，生产环境可以关闭）
                if (totalUsers > 0 && log.isDebugEnabled()) {
                    try {
                        java.util.Set<SimpUser> allUsers = simpUserRegistry.getUsers();
                        java.util.List<String> userIds = allUsers.stream()
                            .map(u -> u.getName())
                            .collect(java.util.stream.Collectors.toList());
                        log.debug("所有在线用户ID: {}", userIds);
                    } catch (Exception e) {
                        // 忽略异常，不影响主流程
                    }
                }
            }
            
            return online;
        } catch (Exception e) {
            log.warn("检查用户在线状态失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }
}

