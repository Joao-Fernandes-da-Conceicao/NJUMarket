package com.njumarket.admin.filter;

import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.utils.UserHolder;
import com.njumarket.admin.repository.AdminRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 管理员上下文Filter（Admin Service专用）
 * 从Gateway传递的请求头中获取管理员ID，然后直接从数据库查询Admin对象
 * 设置到Spring Security SecurityContext和UserHolder中，供Controller和Service层使用
 * 
 * 注意：Admin Service不需要通过Feign Client调用自己，可以直接从数据库查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminContextFilter extends OncePerRequestFilter {

    private final AdminRepository adminRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 从请求头获取管理员ID（Gateway已添加）
            String adminId = request.getHeader("X-Admin-Id");
            String requestURI = request.getRequestURI();
            
            log.debug("AdminContextFilter处理请求: uri={}, X-Admin-Id={}", requestURI, adminId);
            
            // 登录接口不需要处理管理员上下文
            if (requestURI.equals("/api/admin/login")) {
                filterChain.doFilter(request, response);
                return;
            }
            
            if (StringUtils.hasText(adminId)) {
                // 直接从数据库查询管理员（Admin Service不需要通过Feign Client）
                try {
                    log.debug("从数据库查询管理员: adminId={}", adminId);
                    Optional<Admin> adminOpt = adminRepository.findById(adminId);
                    
                    if (adminOpt.isPresent()) {
                        Admin admin = adminOpt.get();
                        
                        // ✅ 检查管理员账户状态
                        String accountStatus = admin.getAccountStatus();
                        if (accountStatus == null || accountStatus.trim().isEmpty()) {
                            log.error("管理员账户状态异常（为null或空）: adminId={}, uri={}, accountStatus原始值=[{}]", 
                                admin.getAdminId(), requestURI, accountStatus);
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"errorMsg\":\"账户状态异常，请联系系统管理员\"}");
                            return;
                        }
                        
                        if (!"ACTIVE".equals(accountStatus)) {
                            String statusMessage = getAccountStatusMessage(accountStatus);
                            log.error("管理员账户已被禁用: adminId={}, status=[{}], uri={}", 
                                admin.getAdminId(), accountStatus, requestURI);
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(String.format("{\"success\":false,\"errorMsg\":\"%s\"}", statusMessage));
                            return;
                        }
                        
                        // 1. 设置Spring Security SecurityContext（用于@PreAuthorize和@CurrentAdmin注解）
                        // 根据adminLevel设置角色：system -> ROLE_SYSTEM, administrator -> ROLE_ADMINISTRATOR
                        String role = "ROLE_" + (admin.isSystemAdmin() ? "SYSTEM" : "ADMINISTRATOR");
                        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority(role)
                        );
                        
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                admin, null, authorities
                            );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // 2. 同时设置UserHolder（向后兼容，用于Service层）
                        UserHolder.saveAdmin(admin);
                        
                        log.debug("管理员信息已设置: adminId={}, username={}, adminLevel={}, accountStatus={}, SecurityContext已设置", 
                            admin.getAdminId(), admin.getUsername(), admin.getAdminLevel(), admin.getAccountStatus());
                    } else {
                        log.warn("管理员不存在: adminId={}, uri={}", adminId, requestURI);
                    }
                } catch (Exception e) {
                    log.error("查询管理员信息时发生异常: adminId={}, error={}", adminId, e.getMessage(), e);
                }
            } else {
                log.debug("请求头中未找到X-Admin-Id: uri={}", requestURI);
            }
            
            // 继续处理请求
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理SecurityContext和ThreadLocal，避免内存泄漏
            SecurityContextHolder.clearContext();
            UserHolder.removeAdmin();
        }
    }
    
    /**
     * 根据账户状态获取管理员友好的提示信息
     */
    private String getAccountStatusMessage(String accountStatus) {
        if (accountStatus == null) {
            return "账户状态异常，请联系系统管理员";
        }
        
        return switch (accountStatus) {
            case "SUSPENDED" -> "账户已被暂停，请联系系统管理员了解详情";
            case "BANNED" -> "账户已被封禁，如有疑问请联系系统管理员";
            default -> "账户状态异常，请联系系统管理员";
        };
    }
}

