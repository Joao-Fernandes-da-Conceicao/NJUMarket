package com.njumarket.auth.utils;

import com.njumarket.auth.entity.User;

/**
 * 用户上下文工具类
 * 更新：优先从Spring Security SecurityContext获取用户，保持向后兼容
 * 
 * 注意：管理员相关逻辑已迁移到 admin 服务
 */
public class UserHolder {
    private static final ThreadLocal<User> tl = new ThreadLocal<>();

    // 用户相关方法
    public static void saveUser(User user){
        tl.set(user);
    }

    /**
     * 获取当前用户
     * 优先从Spring Security SecurityContext获取，如果没有则从ThreadLocal获取（向后兼容）
     * 支持在没有Spring Security的环境中工作（从ThreadLocal获取）
     */
    public static User getUser(){
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
            // Spring Security 不可用，继续使用 ThreadLocal
        }

        // 向后兼容：从ThreadLocal获取（用于Service层等非Controller场景）
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}

