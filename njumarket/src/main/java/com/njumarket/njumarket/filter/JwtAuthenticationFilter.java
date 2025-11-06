package com.njumarket.njumarket.filter;

import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.repository.UserRepository;
import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.utils.RedisConstants;
import com.njumarket.njumarket.utils.UserHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证Filter - 使用Spring Security Filter链
 * 复用原有的JWT验证逻辑，保持向后兼容
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                   @NonNull HttpServletResponse response, 
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // 性能优化：只处理用户相关路径（/api/user/** 和 /api/contact/**）
        // 注意：SecurityConfig中已配置这些路径需要认证，这里的检查是为了性能优化（提前跳过不需要处理的路径）
        String requestURI = request.getRequestURI();
        if (!requestURI.startsWith("/api/user/") && !requestURI.startsWith("/api/contact/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 排除认证相关接口（这些接口在SecurityConfig中配置为permitAll，不需要JWT验证）
        if (isAuthEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 1. 从请求头获取Token（与原有LoginInterceptor逻辑一致）
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("请求缺少Authorization头: {}", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"用户未登录，请先登录\"}");
            return;
        }
        
        // 2. 验证Token有效性（先验证JWT本身）
        if (!jwtUtils.validateToken(token)) {
            log.warn("Token验证失败（JWT过期）: {}", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"Token无效或已过期，请重新登录\"}");
            return;
        }
        
        // 3. 从Token中获取用户ID
        String userId = jwtUtils.getUserIdFromToken(token);
        if (!StringUtils.hasText(userId)) {
            log.warn("Token中无法获取用户ID: {}", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"Token格式错误，请重新登录\"}");
            return;
        }
        
        // 4. 验证Redis中的Token（检查是否被撤销或登出）
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + userId;
        String cachedToken = stringRedisTemplate.opsForValue().get(tokenKey);
        
        // 如果Redis中没有Token，说明用户已登出或Token被撤销
        if (!StringUtils.hasText(cachedToken)) {
            log.warn("Token已被撤销或用户已登出: userId={}, uri={}", userId, requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"Token已失效，请重新登录\"}");
            return;
        }
        
        // 5. 验证Redis中的Token是否与请求中的Token一致（防止Token被替换）
        if (!token.equals(cachedToken)) {
            log.warn("Token不匹配（可能在其他设备登录）: userId={}, uri={}", userId, requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"Token已失效，请重新登录\"}");
            return;
        }
        
        // 注意：不再进行Token续期
        // JWT本身已有24小时过期时间，过期后应使用RefreshToken刷新，而不是续期
        // Redis的作用是验证Token是否被撤销（登出）和防止Token被替换，不需要续期
        
        // 6. 查询用户信息
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"用户账号不存在，请重新登录\"}");
            return;
        }
        
        // 8. 检查用户状态
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            String statusMessage = getAccountStatusMessage(user.getAccountStatus());
            log.warn("用户账户已被禁用: userId={}, status={}", userId, user.getAccountStatus());
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format("{\"success\":false,\"errorMsg\":\"%s\"}", statusMessage));
            return;
        }
        
        // 9. 设置Spring Security的Authentication
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                user, null, Collections.emptyList()
            );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 10. 保持向后兼容：设置UserHolder（Service层等非Controller场景仍可能使用）
        UserHolder.saveUser(user);
        
        try {
            // 11. 继续Filter链
            filterChain.doFilter(request, response);
        } finally {
            // 12. 清理ThreadLocal（与原有LoginInterceptor的afterCompletion一致）
            UserHolder.removeUser();
        }
    }
    
    /**
     * 从请求中获取Token（与原有LoginInterceptor逻辑完全一致）
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 1. 从Authorization头获取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 2. 从请求参数获取（备用方案）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }
    
    /**
     * 判断是否为认证端点（不需要JWT验证）
     */
    private boolean isAuthEndpoint(String requestURI) {
        return requestURI.equals("/api/user/auth/login") ||
               requestURI.equals("/api/user/auth/register") ||
               requestURI.equals("/api/user/auth/register-new") ||
               requestURI.equals("/api/user/auth/send-code") ||
               requestURI.equals("/api/user/auth/login-by-code") ||
               requestURI.equals("/api/user/auth/login-third-party") ||
               requestURI.equals("/api/user/auth/reset-password") ||
               requestURI.equals("/api/user/auth/refresh-token"); // ✅ Token刷新接口不需要JWT验证
    }
    
    /**
     * 根据账户状态获取用户友好的提示信息（与原有LoginInterceptor逻辑一致）
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
