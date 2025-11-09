package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.model.IUser;
import com.njumarket.njumarket.model.IAdmin;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;

/**
 * 安全工具类（Common模块）
 * 提供用户认证、鉴权相关的便捷方法
 * 使用接口避免反射调用，提高类型安全性和性能
 */
public class SecurityUtils {

    // ========== 用户相关方法 ==========

    /**
     * 获取当前认证的用户
     * 优先从Spring Security SecurityContext获取，如果没有则从UserHolder获取（向后兼容）
     * 
     * @return 当前用户对象（IUser接口），如果未登录返回null
     */
    public static IUser getCurrentUser() {
        // 优先从Spring Security SecurityContext获取
        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            Authentication authentication = context.getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof IUser) {
                    return (IUser) principal;
                }
            }
        }

        // 向后兼容：从UserHolder获取（用于Service层等非Controller场景）
        Object user = UserHolder.getUser();
        return user instanceof IUser ? (IUser) user : null;
    }

    /**
     * 获取当前认证的用户ID
     * 
     * @return 用户ID，如果未登录返回null
     */
    public static String getCurrentUserId() {
        IUser user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 要求当前用户必须登录
     * 
     * @return 当前用户对象（IUser接口）
     * @throws BusinessException 如果用户未登录
     */
    public static IUser requireCurrentUser() {
        IUser user = getCurrentUser();
        if (user == null) {
            throw new BusinessException("用户未登录");
        }
        return user;
    }

    /**
     * 要求当前用户必须登录并返回用户ID
     * 
     * @return 用户ID
     * @throws BusinessException 如果用户未登录
     */
    public static String requireCurrentUserId() {
        IUser user = requireCurrentUser();
        return user.getUserId();
    }

    /**
     * 检查当前用户是否已登录
     * 
     * @return true表示已登录，false表示未登录
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    /**
     * 检查当前用户账户状态是否为ACTIVE
     * 
     * @return true表示账户已激活，false表示未激活或未登录
     */
    public static boolean isUserActive() {
        IUser user = getCurrentUser();
        if (user == null) {
            return false;
        }
        String accountStatus = user.getAccountStatus();
        return "ACTIVE".equals(accountStatus);
    }

    /**
     * 要求当前用户账户必须为ACTIVE状态
     * 
     * @return 当前用户对象（IUser接口）
     * @throws BusinessException 如果用户未登录或账户未激活
     */
    public static IUser requireActiveUser() {
        IUser user = requireCurrentUser();
        String accountStatus = user.getAccountStatus();
        if (!"ACTIVE".equals(accountStatus)) {
            throw new BusinessException("账户已被禁用，无法执行此操作");
        }
        return user;
    }

    /**
     * 检查当前用户是否为指定用户
     * 
     * @param userId 要检查的用户ID
     * @return true表示是当前用户，false表示不是或未登录
     */
    public static boolean isCurrentUser(String userId) {
        if (userId == null) {
            return false;
        }
        String currentUserId = getCurrentUserId();
        return userId.equals(currentUserId);
    }

    /**
     * 要求当前用户必须是指定用户
     * 
     * @param userId 要求的用户ID
     * @throws BusinessException 如果当前用户不是指定用户
     */
    public static void requireCurrentUser(String userId) {
        if (!isCurrentUser(userId)) {
            throw new BusinessException("无权限操作此资源");
        }
    }

    // ========== 管理员相关方法 ==========

    /**
     * 获取当前认证的管理员
     * 优先从Spring Security SecurityContext获取，如果没有则从UserHolder获取（向后兼容）
     * 
     * @return 当前管理员对象（IAdmin接口），如果未登录返回null
     */
    public static IAdmin getCurrentAdmin() {
        // 优先从Spring Security SecurityContext获取
        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            Authentication authentication = context.getAuthentication();
            if (authentication != null) {
                // 先获取 Principal（不依赖 isAuthenticated()）
                // 因为即使 isAuthenticated() 返回 false，Principal 也可能有效
                Object principal = authentication.getPrincipal();
                if (principal instanceof IAdmin) {
                    return (IAdmin) principal;
                }
            }
        }

        // 向后兼容：从UserHolder获取（用于Service层等非Controller场景）
        Object admin = UserHolder.getAdmin();
        return admin instanceof IAdmin ? (IAdmin) admin : null;
    }

    /**
     * 获取当前认证的管理员ID
     * 
     * @return 管理员ID，如果未登录返回null
     */
    public static String getCurrentAdminId() {
        IAdmin admin = getCurrentAdmin();
        return admin != null ? admin.getAdminId() : null;
    }

    /**
     * 要求当前管理员必须登录
     * 
     * @return 当前管理员对象（IAdmin接口）
     * @throws BusinessException 如果管理员未登录
     */
    public static IAdmin requireCurrentAdmin() {
        IAdmin admin = getCurrentAdmin();
        if (admin == null) {
            throw new BusinessException("管理员未登录");
        }
        return admin;
    }

    /**
     * 要求当前管理员必须登录并返回管理员ID
     * 
     * @return 管理员ID
     * @throws BusinessException 如果管理员未登录
     */
    public static String requireCurrentAdminId() {
        IAdmin admin = requireCurrentAdmin();
        return admin.getAdminId();
    }

    /**
     * 检查当前管理员是否已登录
     * 
     * @return true表示已登录，false表示未登录
     */
    public static boolean isAdminAuthenticated() {
        return getCurrentAdmin() != null;
    }

    /**
     * 检查当前管理员是否为SYSTEM角色
     * 
     * @return true表示是SYSTEM管理员，false表示不是或未登录
     */
    public static boolean isSystemAdmin() {
        IAdmin admin = getCurrentAdmin();
        if (admin == null) {
            return false;
        }
        Boolean result = admin.isSystemAdmin();
        return Boolean.TRUE.equals(result);
    }

    /**
     * 要求当前管理员必须为SYSTEM角色
     * 
     * @return 当前管理员对象（IAdmin接口）
     * @throws BusinessException 如果管理员未登录或不是SYSTEM角色
     */
    public static IAdmin requireSystemAdmin() {
        IAdmin admin = requireCurrentAdmin();
        Boolean isSystem = admin.isSystemAdmin();
        if (!Boolean.TRUE.equals(isSystem)) {
            throw new BusinessException("无权限执行此操作，需要SYSTEM管理员权限");
        }
        return admin;
    }

    /**
     * 检查当前管理员是否拥有指定权限
     * 
     * @param permission 权限名称
     * @return true表示拥有权限，false表示没有或未登录
     */
    public static boolean hasPermission(String permission) {
        IAdmin admin = getCurrentAdmin();
        if (admin == null) {
            return false;
        }
        Boolean result = admin.hasPermission(permission);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 要求当前管理员必须拥有指定权限
     * 
     * @param permission 权限名称
     * @throws BusinessException 如果管理员未登录或不拥有权限
     */
    public static void requirePermission(String permission) {
        IAdmin admin = requireCurrentAdmin();
        Boolean hasPerm = admin.hasPermission(permission);
        if (!Boolean.TRUE.equals(hasPerm)) {
            throw new BusinessException("无权限执行此操作，需要权限: " + permission);
        }
    }

    /**
     * 检查当前管理员是否为指定管理员
     * 
     * @param adminId 要检查的管理员ID
     * @return true表示是当前管理员，false表示不是或未登录
     */
    public static boolean isCurrentAdmin(String adminId) {
        if (adminId == null) {
            return false;
        }
        String currentAdminId = getCurrentAdminId();
        return adminId.equals(currentAdminId);
    }

    /**
     * 要求当前管理员必须是指定管理员或是SYSTEM管理员
     * 
     * @param adminId 要求的管理员ID
     * @throws BusinessException 如果当前管理员不是指定管理员且不是SYSTEM管理员
     */
    public static void requireCurrentAdminOrSystem(String adminId) {
        IAdmin admin = requireCurrentAdmin();
        Boolean isSystem = admin.isSystemAdmin();
        boolean isCurrent = isCurrentAdmin(adminId);
        if (!Boolean.TRUE.equals(isSystem) && !isCurrent) {
            throw new BusinessException("无权限操作此资源");
        }
    }

    // ========== Spring Security相关方法 ==========

    /**
     * 获取当前认证信息
     * 
     * @return Authentication对象，如果未认证返回null
     */
    public static Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        return context != null ? context.getAuthentication() : null;
    }

    /**
     * 检查当前是否有认证信息
     * 
     * @return true表示已认证，false表示未认证
     */
    public static boolean hasAuthentication() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 获取当前用户的所有权限
     * 
     * @return 权限集合，如果未认证返回空集合
     */
    public static Collection<? extends GrantedAuthority> getAuthorities() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return Collections.emptyList();
        }
        return authentication.getAuthorities();
    }

    /**
     * 检查当前用户是否拥有指定角色
     * 
     * @param role 角色名称（如 "ROLE_ADMIN", "SYSTEM"）
     * @return true表示拥有角色，false表示没有或未认证
     */
    public static boolean hasRole(String role) {
        if (role == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = getAuthorities();
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        GrantedAuthority roleAuthority = new SimpleGrantedAuthority(roleWithPrefix);
        return authorities.contains(roleAuthority);
    }

    /**
     * 要求当前用户必须拥有指定角色
     * 
     * @param role 角色名称
     * @throws BusinessException 如果用户未认证或不拥有角色
     */
    public static void requireRole(String role) {
        if (!hasRole(role)) {
            throw new BusinessException("无权限执行此操作，需要角色: " + role);
        }
    }

    /**
     * 检查当前用户是否拥有指定权限
     * 
     * @param authority 权限名称
     * @return true表示拥有权限，false表示没有或未认证
     */
    public static boolean hasAuthority(String authority) {
        if (authority == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = getAuthorities();
        GrantedAuthority authorityObj = new SimpleGrantedAuthority(authority);
        return authorities.contains(authorityObj);
    }

    /**
     * 要求当前用户必须拥有指定权限
     * 
     * @param authority 权限名称
     * @throws BusinessException 如果用户未认证或不拥有权限
     */
    public static void requireAuthority(String authority) {
        if (!hasAuthority(authority)) {
            throw new BusinessException("无权限执行此操作，需要权限: " + authority);
        }
    }

    // ========== 清理方法 ==========

    /**
     * 清理当前线程的安全上下文
     * 用于请求结束后清理，避免内存泄漏
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
        UserHolder.clearAll();
    }
}

