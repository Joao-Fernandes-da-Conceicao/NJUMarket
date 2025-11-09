package com.njumarket.auth.utils;

import com.njumarket.auth.entity.User;
import com.njumarket.njumarket.exception.BusinessException;

import java.util.Collection;
import java.util.Collections;

/**
 * 安全工具类
 * 提供用户认证、鉴权相关的便捷方法
 * 符合Spring Security标准实践，同时保持向后兼容
 * 
 * 设计原则：
 * 1. 优先使用Spring Security SecurityContext
 * 2. 向后兼容UserHolder（用于Service层等非Controller场景）
 * 3. 提供类型安全的鉴权方法
 * 4. 统一异常处理
 */
public class SecurityUtils {

    // ========== 用户相关方法 ==========

    /**
     * 获取当前认证的用户
     * 优先从Spring Security SecurityContext获取，如果没有则从UserHolder获取（向后兼容）
     * 支持在没有Spring Security的环境中工作（从ThreadLocal获取）
     * 
     * @return 当前用户，如果未登录返回null
     */
    public static User getCurrentUser() {
        // 优先从Spring Security SecurityContext获取（如果Spring Security可用）
        try {
            Object securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolder.getClass().getMethod("getContext").invoke(null);
            Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
            
            if (authentication != null) {
                Boolean isAuthenticated = (Boolean) authentication.getClass().getMethod("isAuthenticated").invoke(authentication);
                if (Boolean.TRUE.equals(isAuthenticated)) {
                    Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
                    if (principal instanceof User) {
                        return (User) principal;
                    }
                }
            }
        } catch (Exception e) {
            // Spring Security 不可用，继续使用 UserHolder
        }

        // 向后兼容：从UserHolder获取（用于Service层等非Controller场景）
        return UserHolder.getUser();
    }

    /**
     * 获取当前认证的用户ID
     * 
     * @return 用户ID，如果未登录返回null
     */
    public static String getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 要求当前用户必须登录
     * 
     * @return 当前用户
     * @throws BusinessException 如果用户未登录
     */
    public static User requireCurrentUser() {
        User user = getCurrentUser();
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
        User user = requireCurrentUser();
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
        User user = getCurrentUser();
        return user != null && "ACTIVE".equals(user.getAccountStatus());
    }

    /**
     * 要求当前用户账户必须为ACTIVE状态
     * 
     * @return 当前用户
     * @throws BusinessException 如果用户未登录或账户未激活
     */
    public static User requireActiveUser() {
        User user = requireCurrentUser();
        if (!"ACTIVE".equals(user.getAccountStatus())) {
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

    // ========== Spring Security相关方法 ==========

    /**
     * 获取当前认证信息
     * 
     * @return Authentication对象，如果未认证返回null
     */
    public static Object getAuthentication() {
        try {
            Object securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolder.getClass().getMethod("getContext").invoke(null);
            return context.getClass().getMethod("getAuthentication").invoke(context);
        } catch (Exception e) {
            // Spring Security 不可用
            return null;
        }
    }

    /**
     * 检查当前是否有认证信息
     * 
     * @return true表示已认证，false表示未认证
     */
    public static boolean hasAuthentication() {
        Object authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }
        try {
            Boolean isAuthenticated = (Boolean) authentication.getClass().getMethod("isAuthenticated").invoke(authentication);
            return Boolean.TRUE.equals(isAuthenticated);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取当前用户的所有权限
     * 
     * @return 权限集合，如果未认证返回空集合
     */
    public static Collection<?> getAuthorities() {
        Object authentication = getAuthentication();
        if (authentication == null) {
            return Collections.emptyList();
        }
        try {
            return (Collection<?>) authentication.getClass().getMethod("getAuthorities").invoke(authentication);
        } catch (Exception e) {
            return Collections.emptyList();
        }
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
        Collection<?> authorities = getAuthorities();
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        try {
            Class<?> simpleGrantedAuthorityClass = Class.forName("org.springframework.security.core.authority.SimpleGrantedAuthority");
            Object roleAuthority = simpleGrantedAuthorityClass.getConstructor(String.class).newInstance(roleWithPrefix);
            return authorities.contains(roleAuthority);
        } catch (Exception e) {
            return false;
        }
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
        Collection<?> authorities = getAuthorities();
        try {
            Class<?> simpleGrantedAuthorityClass = Class.forName("org.springframework.security.core.authority.SimpleGrantedAuthority");
            Object authorityObj = simpleGrantedAuthorityClass.getConstructor(String.class).newInstance(authority);
            return authorities.contains(authorityObj);
        } catch (Exception e) {
            return false;
        }
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
        try {
            Object securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            securityContextHolder.getClass().getMethod("clearContext").invoke(null);
        } catch (Exception e) {
            // Spring Security 不可用，忽略
        }
        UserHolder.removeUser();
    }
}

