package com.njumarket.commodity.filter;

import com.njumarket.commodity.entity.User;
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
 * 用户上下文 Filter（Session-Header 模式）
 *
 * Gateway 已完成 Session 验证，并将用户信息注入请求头：
 *   X-User-Id       : userId
 *   X-User-Status   : accountStatus（ACTIVE / SUSPENDED / BANNED ...）
 *   X-Username      : username
 *   X-Phone         : primaryPhone（来自 Redis Session）
 *   X-Authenticated : "true"
 *
 * 本 Filter 直接读取请求头构建轻量 User 对象，不再 Feign 调用 auth-service。
 * 如需其他用户（卖家/买家）的完整资料，使用 UserCacheService（Redis → Feign 回退）。
 */
@Slf4j
@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            if (requestURI.startsWith("/actuator/")) {
                filterChain.doFilter(request, response);
                return;
            }

            String userId = request.getHeader("X-User-Id");
            String accountStatus = request.getHeader("X-User-Status");
            String username = request.getHeader("X-Username");
            String phone = request.getHeader("X-Phone");

            log.debug("UserContextFilter: uri={}, userId={}", requestURI, userId);

            if (StringUtils.hasText(userId)) {
                // 账户状态校验（Gateway 已做，这里二次防御）
                if (!StringUtils.hasText(accountStatus)) {
                    accountStatus = "ACTIVE";
                }
                if (!"ACTIVE".equals(accountStatus)) {
                    log.warn("账户状态异常，拒绝访问: userId={}, status={}, uri={}", userId, accountStatus, requestURI);
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"errorMsg\":\"" + statusMessage(accountStatus) + "\"}");
                    return;
                }

                // 构建轻量 User（来自 Redis Session，由 Gateway 注入请求头）
                User user = new User();
                user.setUserId(userId);
                user.setUsername(username);
                user.setPrimaryPhone(phone);
                user.setAccountStatus(accountStatus);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                UserHolder.saveUser(user);

                log.debug("用户上下文已设置: userId={}, accountStatus={}", userId, accountStatus);
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

