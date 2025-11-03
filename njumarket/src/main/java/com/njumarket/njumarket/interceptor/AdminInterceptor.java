package com.njumarket.njumarket.interceptor;

import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.repository.AdminRepository;
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
 * 管理员拦截器 - 验证管理员身份和权限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final AdminRepository adminRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("管理员拦截器处理请求: {} {}", method, requestURI);
        
        // 1. 从请求头获取Token
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("管理员请求缺少Authorization头: {}", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"管理员未登录，请先登录\"}");
            return false;
        }
        
        // 2. 验证Token有效性
        if (!jwtUtils.validateToken(token)) {
            log.warn("管理员Token验证失败: {}", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期，请重新登录\"}");
            return false;
        }
        
        // 3. 从Token中获取管理员ID
        String adminId = jwtUtils.getUserIdFromToken(token);
        if (!StringUtils.hasText(adminId)) {
            log.warn("Token中无法获取管理员ID: {}", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token格式错误\"}");
            return false;
        }
        
        // 4. 查询管理员信息
        Admin admin = adminRepository.findById(adminId).orElse(null);
        if (admin == null) {
            log.warn("管理员不存在: adminId={}", adminId);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"管理员账号不存在\"}");
            return false;
        }
        
        // 5. 检查管理员状态
        if (!admin.canLogin()) {
            log.warn("管理员账户已被禁用: adminId={}, status={}", adminId, admin.getAccountStatus());
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"管理员账户已被禁用，请联系系统管理员\"}");
            return false;
        }
        
        // 6. 权限检查（针对敏感操作）
        if (isSensitiveOperation(requestURI, method)) {
            if (!hasRequiredPermission(admin, requestURI, method)) {
                log.warn("管理员权限不足: adminId={}, username={}, operation={}", 
                    adminId, admin.getUsername(), method + " " + requestURI);
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"权限不足，无法执行此操作\"}");
                return false;
            }
        }
        
        // 7. 保存管理员信息到ThreadLocal
        UserHolder.saveAdmin(admin);
        
        log.debug("管理员拦截器验证通过: adminId={}, username={}, level={}", 
            admin.getAdminId(), admin.getUsername(), admin.getAdminLevel());
        
        // 8. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除管理员信息
        UserHolder.removeAdmin();
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
    
    /**
     * 判断是否为敏感操作
     */
    private boolean isSensitiveOperation(String requestURI, String method) {
        // 删除操作
        if ("DELETE".equals(method)) {
            return true;
        }
        
        // 创建管理员
        if (requestURI.contains("/api/admin/create")) {
            return true;
        }
        
        // 删除管理员
        if (requestURI.matches("/api/admin/[^/]+$") && "DELETE".equals(method)) {
            return true;
        }
        
        // 重置密码
        if (requestURI.contains("/reset-password")) {
            return true;
        }
        
        // 更新权限
        if (requestURI.contains("/permissions")) {
            return true;
        }
        
        // 更新管理员状态
        if (requestURI.contains("/status")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查管理员是否有执行操作的权限
     */
    private boolean hasRequiredPermission(Admin admin, String requestURI, String method) {
        // 系统管理员拥有所有权限
        if (admin.isSystemAdmin()) {
            return true;
        }
        
        // 普通管理员的权限检查
        if (admin.isAdministrator()) {
            // 普通管理员不能执行以下操作：
            // 1. 创建管理员
            if (requestURI.contains("/api/admin/create")) {
                return false;
            }
            
            // 2. 删除管理员
            if (requestURI.matches("/api/admin/[^/]+$") && "DELETE".equals(method)) {
                return false;
            }
            
            // 3. 重置密码
            if (requestURI.contains("/reset-password")) {
                return false;
            }
            
            // 4. 更新权限
            if (requestURI.contains("/permissions")) {
                return false;
            }
            
            // 5. 更新管理员状态
            if (requestURI.contains("/status")) {
                return false;
            }
            
            // 6. 获取管理员统计信息
            if (requestURI.contains("/statistics")) {
                return false;
            }
            
            // 7. 更新其他管理员的信息（只能更新自己的，除非是system管理员）
            if (requestURI.matches("/api/admin/[^/]+$") && "PUT".equals(method)) {
                // 提取目标管理员ID
                String[] pathParts = requestURI.split("/");
                if (pathParts.length >= 4) {
                    String targetAdminId = pathParts[3];
                    // system管理员可以更新所有管理员信息，普通管理员只能更新自己的
                    if (!admin.isSystemAdmin() && !admin.getAdminId().equals(targetAdminId)) {
                        return false;
                    }
                }
            }
            
            // 8. 管理员列表和详情查询（只有system权限可用）
            if (requestURI.equals("/api/admin/list") && "GET".equals(method)) {
                return false; // 普通管理员不能查看管理员列表
            }
            if (requestURI.matches("/api/admin/[^/]+$") && "GET".equals(method)) {
                // 提取目标管理员ID
                String[] pathParts = requestURI.split("/");
                if (pathParts.length >= 4) {
                    String targetAdminId = pathParts[3];
                    // system管理员可以查看所有管理员信息，普通管理员只能查看自己的
                    if (!admin.isSystemAdmin() && !admin.getAdminId().equals(targetAdminId)) {
                        return false;
                    }
                }
            }
            
            // 9. 完整更新管理员信息（只有system权限可用）
            if (requestURI.contains("/full") && "PUT".equals(method) && requestURI.contains("/api/admin/")) {
                return false; // 普通管理员不能使用updateAdminFull
            }
        }
        
        return true;
    }
}
