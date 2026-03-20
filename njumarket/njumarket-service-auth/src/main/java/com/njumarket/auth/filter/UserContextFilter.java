package com.njumarket.auth.filter;

import com.njumarket.auth.entity.User;
import com.njumarket.njumarket.utils.UserHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 用户上下文 Filter（Auth Service，Session-Header 模式）
 *
 * Auth Service 本身的受保护接口（如 /api/auth/logout、地址管理等）
 * 也通过 Gateway 进来，因此同样可从请求头读取用户信息，不再查库。
 *
 * 登录、注册、验证码等公开接口由 SecurityConfig 放行，不会走到此处。
 */
@Slf4j
@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            if (requestURI.startsWith("/actuator/") || requestURI.startsWith("/api/internal/")) {
                filterChain.doFilter(request, response);
                return;
            }

            String userId = request.getHeader("X-User-Id");
            String accountStatus = request.getHeader("X-User-Status");
            String username = request.getHeader("X-Username");

            if (StringUtils.hasText(userId)) {
                if (!StringUtils.hasText(accountStatus)) accountStatus = "ACTIVE";

                if (!"ACTIVE".equals(accountStatus)) {
                    log.warn("账户状态异常: userId={}, status={}, uri={}", userId, accountStatus, requestURI);
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"errorMsg\":\"" + statusMessage(accountStatus) + "\"}");
                    return;
                }

                // 构建轻量 User（auth 包内 User 实体，仅填写请求头字段）
                User user = new User();
                user.setUserId(userId);
                user.setUsername(username);
                user.setAccountStatus(accountStatus);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                UserHolder.saveUser(user);
            }

            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            UserHolder.removeUser();
        }
    }

    private String statusMessage(String status) {
        if (status == null) return "账户状态异常，请联系管理员";
        return switch (status) {
            case "SUSPENDED" -> "账户已被暂停，请联系管理员了解详情";
            case "BANNED"    -> "账户已被封禁，如有疑问请联系管理员";
            case "DELETED"   -> "账户已被删除，无法使用";
            default          -> "账户状态异常，请联系管理员";
        };
    }
}
