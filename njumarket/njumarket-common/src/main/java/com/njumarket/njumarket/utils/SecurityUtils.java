package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.exception.BusinessException;

import java.util.Collection;
import java.util.Collections;

/**
 * 安全工具类（Common模块）
 * 提供用户认证、鉴权相关的便捷方法
 * 使用反射避免编译时依赖实体类
 */
public class SecurityUtils {

    // ========== 用户相关方法 ==========

    /**
     * 获取当前认证的用户
     * 优先从Spring Security SecurityContext获取，如果没有则从UserHolder获取（向后兼容）
     * 
     * @return 当前用户对象，如果未登录返回null
     */
    public static Object getCurrentUser() {
        // 优先从Spring Security SecurityContext获取（如果Spring Security可用）
        try {
            Object securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolder.getClass().getMethod("getContext").invoke(null);
            Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
            
            if (authentication != null) {
                Boolean isAuthenticated = (Boolean) authentication.getClass().getMethod("isAuthenticated").invoke(authentication);
                if (Boolean.TRUE.equals(isAuthenticated)) {
                    Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
                    if (principal != null) {
                        return principal;
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
        Object user = getCurrentUser();
        if (user == null) {
            return null;
        }
        try {
            return (String) user.getClass().getMethod("getUserId").invoke(user);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 要求当前用户必须登录
     * 
     * @return 当前用户对象
     * @throws BusinessException 如果用户未登录
     */
    public static Object requireCurrentUser() {
        Object user = getCurrentUser();
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
        Object user = requireCurrentUser();
        try {
            return (String) user.getClass().getMethod("getUserId").invoke(user);
        } catch (Exception e) {
            throw new BusinessException("无法获取用户ID");
        }
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
        Object user = getCurrentUser();
        if (user == null) {
            return false;
        }
        try {
            String accountStatus = (String) user.getClass().getMethod("getAccountStatus").invoke(user);
            return "ACTIVE".equals(accountStatus);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 要求当前用户账户必须为ACTIVE状态
     * 
     * @return 当前用户对象
     * @throws BusinessException 如果用户未登录或账户未激活
     */
    public static Object requireActiveUser() {
        Object user = requireCurrentUser();
        try {
            String accountStatus = (String) user.getClass().getMethod("getAccountStatus").invoke(user);
            if (!"ACTIVE".equals(accountStatus)) {
                throw new BusinessException("账户已被禁用，无法执行此操作");
            }
        } catch (java.lang.reflect.InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            if (e.getCause() instanceof BusinessException) {
                throw (BusinessException) e.getCause();
            }
            throw new BusinessException("无法检查账户状态");
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
     * @return 当前管理员对象，如果未登录返回null
     */
    public static Object getCurrentAdmin() {
        // 优先从Spring Security SecurityContext获取（如果Spring Security可用）
        try {
            Object securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolder.getClass().getMethod("getContext").invoke(null);
            Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
            
            if (authentication != null) {
                // ✅ 先获取 Principal（不依赖 isAuthenticated()）
                // 因为即使 isAuthenticated() 返回 false，Principal 也可能有效
                try {
                    Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
                    if (principal != null) {
                        // ✅ Principal 存在，直接返回（不检查 isAuthenticated()）
                        // 这样可以避免因为 authorities 或其他原因导致 isAuthenticated() 返回 false 的问题
                        return principal;
                    }
                } catch (Exception e2) {
                    // 获取 Principal 失败，继续检查 isAuthenticated()
                }
                
                // ✅ 如果 Principal 获取失败，检查 isAuthenticated()
                try {
                    Boolean isAuthenticated = (Boolean) authentication.getClass().getMethod("isAuthenticated").invoke(authentication);
                    if (Boolean.TRUE.equals(isAuthenticated)) {
                        Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
                        if (principal != null) {
                            return principal;
                        }
                    }
                } catch (Exception e3) {
                    // 忽略异常，继续使用 UserHolder
                }
            }
        } catch (Exception e) {
            // Spring Security 不可用或反射调用失败，继续使用 UserHolder
            // 注意：这里不记录日志，因为这是正常的降级行为
        }

        // 向后兼容：从UserHolder获取（用于Service层等非Controller场景）
        return UserHolder.getAdmin();
    }

    /**
     * 获取当前认证的管理员ID
     * 
     * @return 管理员ID，如果未登录返回null
     */
    public static String getCurrentAdminId() {
        Object admin = getCurrentAdmin();
        if (admin == null) {
            return null;
        }
        try {
            return (String) admin.getClass().getMethod("getAdminId").invoke(admin);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 要求当前管理员必须登录
     * 
     * @return 当前管理员对象
     * @throws BusinessException 如果管理员未登录
     */
    public static Object requireCurrentAdmin() {
        Object admin = getCurrentAdmin();
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
        Object admin = requireCurrentAdmin();
        try {
            return (String) admin.getClass().getMethod("getAdminId").invoke(admin);
        } catch (Exception e) {
            throw new BusinessException("无法获取管理员ID");
        }
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
        Object admin = getCurrentAdmin();
        if (admin == null) {
            return false;
        }
        try {
            Boolean result = (Boolean) admin.getClass().getMethod("isSystemAdmin").invoke(admin);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 要求当前管理员必须为SYSTEM角色
     * 
     * @return 当前管理员对象
     * @throws BusinessException 如果管理员未登录或不是SYSTEM角色
     */
    public static Object requireSystemAdmin() {
        Object admin = requireCurrentAdmin();
        try {
            Boolean isSystem = (Boolean) admin.getClass().getMethod("isSystemAdmin").invoke(admin);
            if (!Boolean.TRUE.equals(isSystem)) {
                throw new BusinessException("无权限执行此操作，需要SYSTEM管理员权限");
            }
        } catch (java.lang.reflect.InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            if (e.getCause() instanceof BusinessException) {
                throw (BusinessException) e.getCause();
            }
            throw new BusinessException("无法检查管理员权限");
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
        Object admin = getCurrentAdmin();
        if (admin == null) {
            return false;
        }
        try {
            Boolean result = (Boolean) admin.getClass().getMethod("hasPermission", String.class).invoke(admin, permission);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 要求当前管理员必须拥有指定权限
     * 
     * @param permission 权限名称
     * @throws BusinessException 如果管理员未登录或不拥有权限
     */
    public static void requirePermission(String permission) {
        Object admin = requireCurrentAdmin();
        try {
            Boolean hasPerm = (Boolean) admin.getClass().getMethod("hasPermission", String.class).invoke(admin, permission);
            if (!Boolean.TRUE.equals(hasPerm)) {
                throw new BusinessException("无权限执行此操作，需要权限: " + permission);
            }
        } catch (java.lang.reflect.InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            if (e.getCause() instanceof BusinessException) {
                throw (BusinessException) e.getCause();
            }
            throw new BusinessException("无法检查权限");
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
        Object admin = requireCurrentAdmin();
        try {
            Boolean isSystem = (Boolean) admin.getClass().getMethod("isSystemAdmin").invoke(admin);
            boolean isCurrent = isCurrentAdmin(adminId);
            if (!isSystem && !isCurrent) {
                throw new BusinessException("无权限操作此资源");
            }
        } catch (java.lang.reflect.InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            if (e.getCause() instanceof BusinessException) {
                throw (BusinessException) e.getCause();
            }
            throw new BusinessException("无法检查管理员权限");
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
        UserHolder.clearAll();
    }
}

