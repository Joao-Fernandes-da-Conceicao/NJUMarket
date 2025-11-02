package com.njumarket.njumarket.config;

import com.njumarket.njumarket.websocket.UserPrincipal;
import com.njumarket.njumarket.websocket.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.Map;

/**
 * Spring WebSocket 配置类
 * 
 * 使用 Spring WebSocket 框架，支持更好的依赖注入和消息代理
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    
    /**
     * 配置消息代理
     * 使用简单内存消息代理（生产环境可以配置 RabbitMQ 或 Redis）
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Enable simple in-memory message broker for client subscriptions
        // Broker handles destinations starting with these prefixes
        // NOTE: Do NOT include "/user" here - it's handled by setUserDestinationPrefix()
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for client-to-server messages (not used in current implementation)
        config.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix for point-to-point messages
        // CRITICAL: This must be set for convertAndSendToUser() to work
        // When using convertAndSendToUser(), Spring prepends this prefix
        // Example: convertAndSendToUser("userId", "/queue/message") 
        //          -> sends to /user/userId/queue/message
        // Spring automatically converts client subscription /user/queue/message
        //          -> registered as /user/{Principal.getName()}/queue/message
        config.setUserDestinationPrefix("/user");
    }
    
    /**
     * 配置客户端入站通道拦截器
     * 用于在 CONNECT 帧时设置 Principal，使 convertAndSendToUser() 能够识别用户
     */
    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    StompCommand command = accessor.getCommand();
                    
                    // CRITICAL: Set Principal in CONNECT frame so it's available for all subsequent frames
                    // This ensures SUBSCRIBE frames have Principal when Spring registers subscriptions
                    if (StompCommand.CONNECT.equals(command)) {
                        // Get userId from session attributes (set by handshake interceptor)
                        String userId = null;
                        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                        if (sessionAttributes != null) {
                            userId = (String) sessionAttributes.get("userId");
                        }
                        
                        if (userId != null) {
                            // Create and set Principal early in CONNECT frame
                            UserPrincipal principal = new UserPrincipal(userId);
                            accessor.setUser(principal);
                            log.info("Principal set in CONNECT: userId={}, Principal.getName()={}", 
                                    userId, principal.getName());
                        } else {
                            log.warn("Principal NOT set in CONNECT: userId not found in sessionAttributes");
                        }
                    }
                    
                    // Ensure Principal exists in SUBSCRIBE frame (should already be set from CONNECT)
                    if (StompCommand.SUBSCRIBE.equals(command)) {
                        Principal existingPrincipal = accessor.getUser();
                        String destination = accessor.getDestination();
                        
                        if (existingPrincipal == null) {
                            // Fallback: try to get userId from session attributes
                            String userId = null;
                            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                            if (sessionAttributes != null) {
                                userId = (String) sessionAttributes.get("userId");
                            }
                            
                            if (userId != null) {
                                UserPrincipal principal = new UserPrincipal(userId);
                                accessor.setUser(principal);
                                log.warn("Principal set late in SUBSCRIBE (fallback): userId={}, destination={}", 
                                        userId, destination);
                            } else {
                                log.error("CRITICAL: Principal is NULL in SUBSCRIBE and userId not found! destination={}", 
                                        destination);
                            }
                        } else {
                            log.info("Principal verified in SUBSCRIBE: userId={}, destination={}, Principal.getName()={}", 
                                    existingPrincipal.getName(), destination, existingPrincipal.getName());
                        }
                    }
                }
                
                return message;
            }
        });
    }
    
    /**
     * 注册 STOMP 端点
     */
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // 注册 WebSocket 端点，客户端连接地址：ws://localhost:8080/api/ws
        registry.addEndpoint("/api/ws")
                .setAllowedOrigins("http://localhost:8081", "http://127.0.0.1:8081")
                .addInterceptors(handshakeInterceptor)
                .withSockJS(); // 支持 SockJS（可选，用于浏览器兼容性）
        
        // 同时支持原生 WebSocket（不使用 SockJS）
        registry.addEndpoint("/api/ws")
                .setAllowedOrigins("http://localhost:8081", "http://127.0.0.1:8081")
                .addInterceptors(handshakeInterceptor);
        
        log.info("WebSocket endpoint registered: /api/ws");
    }
}
