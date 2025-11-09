package com.njumarket.commodity.filter;

import com.njumarket.commodity.client.AuthClient;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.commodity.entity.User; // User 实体（Commodity Service专用）
import com.njumarket.njumarket.utils.UserHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.FilterChain;
import java.util.Collections;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
            // 从请求头获取用户ID（Gateway已添加）
            String userId = request.getHeader("X-User-Id");
            String requestURI = request.getRequestURI();
            
            log.debug("UserContextFilter处理请求: uri={}, X-User-Id={}", requestURI, userId);
            
            if (StringUtils.hasText(userId)) {
                // 通过Feign Client获取完整的User对象
                try {
                    log.debug("调用authClient.getUserById: userId={}", userId);
                    Result userResult = authClient.getUserById(userId);
                    log.debug("authClient.getUserById返回: success={}, data={}", 
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
                    } else {
                        log.warn("获取用户信息失败: userId={}, success={}, data={}", 
                            userId, userResult.getSuccess(), userResult.getData() != null ? "not null" : "null");
                    }
                } catch (Exception e) {
                    log.error("获取用户信息时发生异常: userId={}, error={}", userId, e.getMessage(), e);
                }
            } else {
                log.debug("请求头中未找到X-User-Id: uri={}", requestURI);
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

