package com.njumarket.njumarket.websocket;

import com.njumarket.njumarket.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 用于验证 token 和设置用户信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    private final JwtUtils jwtUtils;
    
    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) throws Exception {
        
        // 获取查询参数中的 token
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String queryString = servletRequest.getServletRequest().getQueryString();
            
            String token = extractTokenFromQuery(queryString);
            
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket connection missing token, rejecting");
                return false;
            }
            
            // 验证 token 并获取用户ID
            String userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("WebSocket connection token invalid, rejecting");
                return false;
            }
            
            // 验证 token 是否过期
            if (jwtUtils.isTokenExpired(token)) {
                log.warn("WebSocket connection token expired, rejecting: userId={}", userId);
                return false;
            }
            
            // 将 userId 保存到 attributes 中，后续可以在 WebSocketHandler 中获取
            attributes.put("userId", userId);
            attributes.put("token", token);
            
            // 注意：Principal 将在 ChannelInterceptor 的 CONNECT 帧中设置
            // 这里只需要保存 userId 到 attributes 中即可
            
            log.debug("WebSocket handshake validated: userId={}", userId);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
        // 握手完成后可以执行的操作
        if (exception != null) {
            log.error("WebSocket 握手失败：{}", exception.getMessage());
        }
    }
    
    /**
     * 从查询字符串中提取 token
     */
    private String extractTokenFromQuery(String queryString) {
        if (queryString == null || !queryString.contains("token=")) {
            return null;
        }
        String[] params = queryString.split("&");
        for (String param : params) {
            if (param.startsWith("token=")) {
                return param.substring(6); // "token=".length() = 6
            }
        }
        return null;
    }
}

