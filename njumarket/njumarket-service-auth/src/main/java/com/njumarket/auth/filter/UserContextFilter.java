package com.njumarket.auth.filter;

import com.njumarket.auth.entity.User;
import com.njumarket.njumarket.utils.UserHolder;
import com.njumarket.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
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
import java.util.Collections;
import java.util.Optional;

/**
 * 用户上下文Filter（Auth Service专用）
 * 从Gateway传递的请求头中获取用户ID，然后直接从数据库查询User对象
 * 设置到Spring Security SecurityContext和UserHolder中，供Controller和Service层使用
 * 
 * 注意：Auth Service不需要通过Feign Client调用自己，可以直接从数据库查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 从请求头获取用户ID（Gateway已添加）
            String userId = request.getHeader("X-User-Id");
            String requestURI = request.getRequestURI();
            
            log.info("UserContextFilter处理请求: uri={}, X-User-Id={}, X-User-Id是否为null={}", 
                requestURI, userId, userId == null);
            
            if (StringUtils.hasText(userId)) {
                // 直接从数据库查询用户（Auth Service不需要通过Feign Client）
                try {
                    log.info("从数据库查询用户: userId={}", userId);
                    Optional<User> userOpt = userRepository.findById(userId);
                    
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        
                        // 调试：打印accountStatus的详细信息
                        log.info("从数据库获取的accountStatus: [{}], userId={}, accountStatus是否为null={}", 
                            user.getAccountStatus(), user.getUserId(), user.getAccountStatus() == null);
                        
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
                        log.warn("用户不存在: userId={}, uri={}", userId, requestURI);
                    }
                } catch (Exception e) {
                    log.error("查询用户信息时发生异常: userId={}, error={}", userId, e.getMessage(), e);
                }
            } else {
                log.warn("请求头中未找到X-User-Id: uri={}, 所有请求头={}", requestURI, 
                    Collections.list(request.getHeaderNames()));
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

