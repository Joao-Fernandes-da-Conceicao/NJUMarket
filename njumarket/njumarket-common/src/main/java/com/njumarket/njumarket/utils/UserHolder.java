package com.njumarket.njumarket.utils;

/**
 * 用户上下文工具类（Common模块）
 * 使用ThreadLocal存储用户和管理员信息
 * 使用反射避免编译时依赖实体类
 */
public class UserHolder {
    private static final ThreadLocal<Object> userTl = new ThreadLocal<>();
    private static final ThreadLocal<Object> adminTl = new ThreadLocal<>();

    // 用户相关方法
    public static void saveUser(Object user) {
        userTl.set(user);
    }

    /**
     * 获取当前用户
     * 优先从Spring Security SecurityContext获取，如果没有则从ThreadLocal获取（向后兼容）
     */
    public static Object getUser() {
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
            // Spring Security 不可用，继续使用 ThreadLocal
        }

        // 向后兼容：从ThreadLocal获取（用于Service层等非Controller场景）
        return userTl.get();
    }

    public static void removeUser() {
        userTl.remove();
    }

    // 管理员相关方法
    public static void saveAdmin(Object admin) {
        adminTl.set(admin);
    }

    /**
     * 获取当前管理员
     * 优先从Spring Security SecurityContext获取，如果没有则从ThreadLocal获取（向后兼容）
     */
    public static Object getAdmin() {
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
                    // 忽略异常，继续使用 ThreadLocal
                }
            }
        } catch (Exception e) {
            // Spring Security 不可用，继续使用 ThreadLocal
        }

        // 向后兼容：从ThreadLocal获取（用于Service层等非Controller场景）
        return adminTl.get();
    }

    public static void removeAdmin() {
        adminTl.remove();
    }

    // 清理所有上下文
    public static void clearAll() {
        userTl.remove();
        adminTl.remove();
    }
}

