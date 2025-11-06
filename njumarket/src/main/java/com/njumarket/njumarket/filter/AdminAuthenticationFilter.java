package com.njumarket.njumarket.filter;

import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.repository.AdminRepository;
import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.utils.UserHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员认证Filter - 使用Spring Security Filter链
 * 复用原有的JWT验证逻辑，保持向后兼容
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AdminRepository adminRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                   @NonNull HttpServletResponse response, 
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // 只处理管理员相关路径
        String requestURI = request.getRequestURI();
        if (!requestURI.startsWith("/api/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 排除登录接口
        if (requestURI.equals("/api/admin/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 1. 从请求头获取Token（与原有AdminInterceptor逻辑一致）
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("管理员请求缺少Authorization头: {}", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"管理员未登录，请先登录\"}");
            return;
        }
        
        // 2. 验证Token有效性
        if (!jwtUtils.validateToken(token)) {
            log.warn("管理员Token验证失败: {}", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"Token无效或已过期，请重新登录\"}");
            return;
        }
        
        // 3. 从Token中获取管理员ID
        String adminId = jwtUtils.getUserIdFromToken(token);
        if (!StringUtils.hasText(adminId)) {
            log.warn("Token中无法获取管理员ID: {}", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"Token格式错误，请重新登录\"}");
            return;
        }
        
        // 4. 查询管理员信息
        Admin admin = adminRepository.findById(adminId).orElse(null);
        if (admin == null) {
            log.warn("管理员不存在: adminId={}", adminId);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"管理员账号不存在，请重新登录\"}");
            return;
        }
        
        // 5. 检查管理员状态
        if (!admin.canLogin()) {
            String statusMessage = getAccountStatusMessage(admin.getAccountStatus());
            log.warn("管理员账户已被禁用: adminId={}, status={}", adminId, admin.getAccountStatus());
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format("{\"success\":false,\"errorMsg\":\"%s\"}", statusMessage));
            return;
        }
        
        // 6. 设置Spring Security的Authentication（包含角色信息）
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (admin.isSystemAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SYSTEM"));
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                admin, null, authorities
            );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 7. 保持向后兼容：设置UserHolder（Service层等非Controller场景仍可能使用）
        UserHolder.saveAdmin(admin);
        
        try {
            // 8. 继续Filter链
            filterChain.doFilter(request, response);
        } finally {
            // 9. 清理ThreadLocal（与原有AdminInterceptor的afterCompletion一致）
            UserHolder.removeAdmin();
        }
    }
    
    /**
     * 从请求中获取Token（与原有AdminInterceptor逻辑完全一致）
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
     * 根据账户状态获取用户友好的提示信息（与原有AdminInterceptor逻辑一致）
     */
    private String getAccountStatusMessage(String accountStatus) {
        if (accountStatus == null) {
            return "账户状态异常，请联系系统管理员";
        }
        
        return switch (accountStatus) {
            case "SUSPENDED" -> "管理员账户已被暂停，请联系系统管理员了解详情";
            case "BANNED" -> "管理员账户已被封禁，如有疑问请联系系统管理员";
            case "DELETED" -> "管理员账户已被删除，无法使用";
            default -> "账户状态异常，请联系系统管理员";
        };
    }
}

