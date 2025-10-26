package com.njumarket.njumarket.interceptor;

import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.repository.UserRepository;
import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器 - 基于JWT
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头获取Token
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("请求缺少Authorization头: {}", request.getRequestURI());
            response.setStatus(401);
            return false;
        }
        
        // 2. 验证Token有效性
        if (!jwtUtils.validateToken(token)) {
            log.warn("Token验证失败: {}", request.getRequestURI());
            response.setStatus(401);
            return false;
        }
        
        // 3. 从Token中获取用户信息
        String userId = jwtUtils.getUserIdFromToken(token);
        if (!StringUtils.hasText(userId)) {
            log.warn("Token中无法获取用户ID: {}", request.getRequestURI());
            response.setStatus(401);
            return false;
        }
        
        // 4. 查询用户信息
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            response.setStatus(401);
            return false;
        }
        
        // 5. 检查用户状态
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            log.warn("用户账户已被禁用: userId={}, status={}", userId, user.getAccountStatus());
            response.setStatus(403);
            return false;
        }
        
        // 6. 保存用户信息到ThreadLocal
        UserHolder.saveUser(user);
        
        // 7. 设置userId到request attribute（供Controller使用）
        request.setAttribute("userId", userId);
        
        // 8. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户信息
        UserHolder.removeUser();
    }
    
    /**
     * 从请求中获取Token
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
}
