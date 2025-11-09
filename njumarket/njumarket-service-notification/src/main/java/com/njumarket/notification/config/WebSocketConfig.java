package com.njumarket.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket配置类（推送服务）
 * 统一管理所有WebSocket连接：订单通知、商品通知、聊天消息等
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理，用于向客户端发送消息
        // 客户端订阅 /user/queue/notification 来接收所有通知
        config.enableSimpleBroker("/queue", "/topic");
        
        // 设置应用程序消息的前缀
        // 客户端发送消息到 /app/xxx
        config.setApplicationDestinationPrefixes("/app");
        
        // 设置用户目标前缀
        // 服务器发送消息到 /user/{userId}/queue/notification
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册统一的STOMP端点，客户端通过这个端点连接WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 允许所有来源（生产环境应配置具体域名）
                .addInterceptors(new WebSocketHandshakeInterceptor()) // 添加握手拦截器，提取userId
                .withSockJS(); // 启用SockJS支持，提供降级方案
    }
    
    /**
     * 配置客户端入站通道拦截器
     * 用于在STOMP CONNECT消息时设置Principal
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketChannelInterceptor());
    }
    
    /**
     * WebSocket握手拦截器
     * 从HTTP请求头中提取X-User-Id，并存储到attributes中
     */
    @Slf4j
    static class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
        
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            // 从请求头获取用户ID（Gateway已添加）
            String userId = request.getHeaders().getFirst("X-User-Id");
            
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("WebSocket握手（推送服务）: 提取userId={}", userId);
                
                // 将userId存储到attributes中，供ChannelInterceptor使用
                attributes.put("userId", userId);
                
                log.info("WebSocket握手（推送服务）: 已存储userId到attributes, userId={}", userId);
                return true;
            } else {
                log.warn("WebSocket握手（推送服务）: 未找到X-User-Id请求头，拒绝连接");
                return false; // 拒绝连接
            }
        }
        
        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Exception exception) {
            if (exception != null) {
                log.error("WebSocket握手失败（推送服务）: error={}", exception.getMessage(), exception);
            } else {
                String userId = request.getHeaders().getFirst("X-User-Id");
                log.info("WebSocket握手完成（推送服务）: userId={}", userId);
            }
        }
    }
    
    /**
     * WebSocket通道拦截器
     * 在STOMP CONNECT消息时，从attributes中获取userId并设置为Principal
     * 这样SimpUserRegistry.getUser(userId)才能正确找到用户
     */
    @Slf4j
    static class WebSocketChannelInterceptor implements ChannelInterceptor {
        
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            
            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                // 从attributes中获取userId（由HandshakeInterceptor设置）
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null) {
                    String userId = (String) sessionAttributes.get("userId");
                    
                    if (userId != null && !userId.trim().isEmpty()) {
                        log.info("WebSocket CONNECT（推送服务）: 设置Principal, userId={}", userId);
                        
                        // 创建Principal对象，name为userId
                        // 这样SimpUserRegistry.getUser(userId)才能正确找到用户
                        Principal principal = new Principal() {
                            @Override
                            public String getName() {
                                return userId;
                            }
                        };
                        
                        // 设置Principal到StompHeaderAccessor
                        accessor.setUser(principal);
                        
                        log.info("WebSocket CONNECT（推送服务）: Principal已设置, name={}", userId);
                    } else {
                        log.warn("WebSocket CONNECT（推送服务）: 未找到userId in sessionAttributes");
                    }
                } else {
                    log.warn("WebSocket CONNECT（推送服务）: sessionAttributes为null");
                }
            }
            
            return message;
        }
    }
}

