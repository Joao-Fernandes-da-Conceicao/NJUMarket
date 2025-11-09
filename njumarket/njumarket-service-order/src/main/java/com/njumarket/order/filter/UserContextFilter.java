package com.njumarket.order.filter;

import com.njumarket.order.client.AuthClient;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.order.entity.User; // User 实体（Order Service专用）
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
 * 2. 同时设置SecurityContext（用于@CurrentUser注解）和UserHolder（向后兼容）
 * 3. 确保SecurityUtils和@CurrentUser注解正常工作
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
            
            // ⚠️ 重要：WebSocket 握手请求由 WebSocketHandshakeInterceptor 处理
            // UserContextFilter 不应该阻止 WebSocket 握手，即使获取用户信息失败也要允许通过
            // WebSocket 握手时，X-User-Id 会由 Gateway 传递，WebSocketHandshakeInterceptor 会提取
            boolean isWebSocketHandshake = requestURI.startsWith("/ws/order/") || 
                                          requestURI.contains("/websocket") ||
                                          "websocket".equalsIgnoreCase(request.getHeader("Upgrade"));
            
            // 从请求头获取用户ID（Gateway已添加）
            String userId = request.getHeader("X-User-Id");
            
            log.info("UserContextFilter处理请求: uri={}, X-User-Id={}, isWebSocketHandshake={}", 
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
                    log.info("调用authClient.getUserById: userId={}", userId);
                    Result userResult = authClient.getUserById(userId);
                    log.info("authClient.getUserById返回: success={}, data={}", 
                        userResult.getSuccess(), userResult.getData() != null ? "not null" : "null");
                    
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
                        
                        // 调试：打印accountStatus的详细信息
                        log.info("从auth-service获取的accountStatus: [{}], userId={}, accountStatus是否为null={}", 
                            userDTO.getAccountStatus(), userDTO.getUserId(), userDTO.getAccountStatus() == null);
                        
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
                            log.error("用户账户状态异常（为null或空）: userId={}, uri={}, accountStatus原始值=[{}]", 
                                user.getUserId(), requestURI, accountStatus);
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"errorMsg\":\"账户状态异常，请联系管理员\"}");
                            log.info("已返回403响应: userId={}, accountStatus为null或空", user.getUserId());
                            return;
                        }
                        
                        if (!"ACTIVE".equals(accountStatus)) {
                            String statusMessage = getAccountStatusMessage(accountStatus);
                            log.error("用户账户已被禁用: userId={}, status=[{}], status是否为null={}, uri={}", 
                                user.getUserId(), accountStatus, accountStatus == null, requestURI);
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(String.format("{\"success\":false,\"errorMsg\":\"%s\"}", statusMessage));
                            log.info("已返回403响应: userId={}, status={}, message={}", 
                                user.getUserId(), accountStatus, statusMessage);
                            return;
                        }
                        
                        // 1. 设置Spring Security SecurityContext（用于@CurrentUser注解）
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
                    } else {
                        log.warn("获取用户信息失败: userId={}, success={}, data={}", 
                            userId, userResult.getSuccess(), userResult.getData() != null ? "not null" : "null");
                    }
                } catch (Exception e) {
                    log.error("获取用户信息时发生异常: userId={}, error={}", userId, e.getMessage(), e);
                }
            } else {
                log.warn("请求头中未找到X-User-Id: uri={}, 所有请求头={}", requestURI, 
                    java.util.Collections.list(request.getHeaderNames()));
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

