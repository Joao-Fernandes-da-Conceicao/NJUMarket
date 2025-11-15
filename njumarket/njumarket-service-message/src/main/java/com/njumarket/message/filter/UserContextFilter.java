package com.njumarket.message.filter;

import com.njumarket.message.client.AuthClient;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.message.entity.User; // User 实体（Message Service专用）
import com.njumarket.njumarket.utils.UserHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * 用户上下文Filter
 * 从Gateway传递的请求头中获取用户ID，然后通过Feign Client获取完整的User对象
 * 设置到Spring Security SecurityContext和UserHolder中，供Controller和Service层使用
 * 
 * 重构说明：
 * 1. 参考单体版（1.4.1）的JwtAuthenticationFilter实现
 * 2. 同时设置SecurityContext（用于@AuthenticationPrincipal注解）和UserHolder（向后兼容）
 * 3. 确保SecurityUtils和@AuthenticationPrincipal注解正常工作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextFilter extends OncePerRequestFilter {

    private final AuthClient authClient;
    
    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            
            // ✅ 排除 Actuator 端点（这些端点不需要用户认证，也不需要设置用户上下文）
            // Prometheus 等监控工具会直接访问这些端点，不会经过 Gateway，因此没有 X-User-Id
            if (requestURI.startsWith("/actuator/")) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // ⚠️ 重要：WebSocket 握手请求由 WebSocketHandshakeInterceptor 处理
            // UserContextFilter 不应该阻止 WebSocket 握手，即使获取用户信息失败也要允许通过
            // WebSocket 握手时，X-User-Id 会由 Gateway 传递，WebSocketHandshakeInterceptor 会提取
            boolean isWebSocketHandshake = requestURI.startsWith("/ws/") || 
                                          requestURI.contains("/websocket") ||
                                          "websocket".equalsIgnoreCase(request.getHeader("Upgrade"));
            
            // 从请求头获取用户ID（Gateway已添加）
            String userId = request.getHeader("X-User-Id");
            
            log.debug("UserContextFilter处理请求: uri={}, X-User-Id={}, isWebSocketHandshake={}", 
                requestURI, userId, isWebSocketHandshake);
            
            // WebSocket 握手请求：只设置 SecurityContext，不进行账户状态检查
            // 账户状态检查由 WebSocketHandshakeInterceptor 在握手时进行
            if (isWebSocketHandshake && StringUtils.hasText(userId)) {
                // WebSocket 握手时，只设置基本的用户信息，不进行账户状态检查
                // 这样可以避免因为 Feign Client 调用失败而阻止 WebSocket 连接
                log.debug("WebSocket握手请求，跳过用户信息获取: uri={}, userId={}", requestURI, userId);
                // 继续处理，让 WebSocketHandshakeInterceptor 处理认证
            } else if (StringUtils.hasText(userId)) {
                // 普通 HTTP 请求：获取完整的用户信息并检查账户状态
                // 通过Feign Client获取完整的User对象
                try {
                    Result userResult = authClient.getUserById(userId);
                    if (userResult.getSuccess() && userResult.getData() != null) {
                        // 将UserInternalDTO转换为User对象
                        UserInternalDTO userDTO;
                        if (objectMapper != null) {
                            userDTO = objectMapper.convertValue(
                                userResult.getData(),
                                new TypeReference<UserInternalDTO>() {}
                            );
                        } else {
                            // 如果ObjectMapper不可用，使用手动转换
                            @SuppressWarnings("unchecked")
                            Map<String, Object> dataMap = (Map<String, Object>) userResult.getData();
                            userDTO = new UserInternalDTO();
                            userDTO.setUserId((String) dataMap.get("userId"));
                            userDTO.setUsername((String) dataMap.get("username"));
                            userDTO.setPrimaryPhone((String) dataMap.get("primaryPhone"));
                            userDTO.setAccountStatus((String) dataMap.get("accountStatus"));
                            userDTO.setRegisterTime((java.time.LocalDateTime) dataMap.get("registerTime"));
                        }
                        
                        // 创建User对象（只包含基本信息）
                        User user = new User();
                        user.setUserId(userDTO.getUserId());
                        user.setUsername(userDTO.getUsername());
                        user.setPrimaryPhone(userDTO.getPrimaryPhone());
                        user.setAccountStatus(userDTO.getAccountStatus());
                        user.setRegisterTime(userDTO.getRegisterTime());
                        
                        // ✅ 检查用户账户状态（参考单体版JwtAuthenticationFilter的实现）
                        // 如果accountStatus为null或空字符串，也视为异常状态
                        String accountStatus = user.getAccountStatus();
                        if (accountStatus == null || accountStatus.trim().isEmpty()) {
                            log.error("用户账户状态异常（为null或空）: userId={}, uri={}", user.getUserId(), requestURI);
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"errorMsg\":\"账户状态异常，请联系管理员\"}");
                            return;
                        }
                        
                        if (!"ACTIVE".equals(accountStatus)) {
                            String statusMessage = getAccountStatusMessage(accountStatus);
                            log.warn("用户账户已被禁用: userId={}, status=[{}], uri={}", 
                                user.getUserId(), accountStatus, requestURI);
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(String.format("{\"success\":false,\"errorMsg\":\"%s\"}", statusMessage));
                            return;
                        }
                        
                        // 1. 设置Spring Security SecurityContext（用于@AuthenticationPrincipal注解）
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                user, null, Collections.emptyList()
                            );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // 2. 同时设置UserHolder（向后兼容，用于Service层）
                        UserHolder.saveUser(user);
                        
                        log.info("用户信息已设置: userId={}, username={}, accountStatus={}, SecurityContext已设置", 
                            user.getUserId(), user.getUsername(), user.getAccountStatus());
                    }
                } catch (Exception e) {
                    log.warn("获取用户信息失败: userId={}, error={}", userId, e.getMessage());
                }
            }
            
            // 继续处理请求
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理SecurityContext和ThreadLocal，避免内存泄漏
            SecurityContextHolder.clearContext();
            UserHolder.removeUser();
        }
    }
    
    /**
     * 根据账户状态获取用户友好的提示信息（参考单体版JwtAuthenticationFilter的实现）
     */
    private String getAccountStatusMessage(String accountStatus) {
        if (accountStatus == null) {
            return "账户状态异常，请联系管理员";
        }
        
        return switch (accountStatus) {
            case "SUSPENDED" -> "账户已被暂停，请联系管理员了解详情";
            case "BANNED" -> "账户已被封禁，如有疑问请联系管理员";
            case "DELETED" -> "账户已被删除，无法使用";
            default -> "账户状态异常，请联系管理员";
        };
    }
}

