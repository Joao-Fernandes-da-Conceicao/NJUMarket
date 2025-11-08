package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.Admin;

/**
 * 用户上下文工具类
 * 更新：优先从Spring Security SecurityContext获取用户，保持向后兼容
 */
public class UserHolder {
    private static final ThreadLocal<User> tl = new ThreadLocal<>();
    private static final ThreadLocal<Admin> adminTl = new ThreadLocal<>();

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

    // 管理员相关方法
    public static void saveAdmin(Admin admin){
        adminTl.set(admin);
    }

    /**
     * 获取当前管理员
     * 优先从Spring Security SecurityContext获取，如果没有则从ThreadLocal获取（向后兼容）
     * 支持在没有Spring Security的环境中工作（从ThreadLocal获取）
     */
    public static Admin getAdmin(){
        // 优先从Spring Security SecurityContext获取（如果Spring Security可用）
        try {
            Object securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolder.getClass().getMethod("getContext").invoke(null);
            Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
            
            if (authentication != null) {
                Boolean isAuthenticated = (Boolean) authentication.getClass().getMethod("isAuthenticated").invoke(authentication);
                if (Boolean.TRUE.equals(isAuthenticated)) {
                    Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
                    if (principal instanceof Admin) {
                        return (Admin) principal;
                    }
                }
            }
        } catch (Exception e) {
            // Spring Security 不可用，继续使用 ThreadLocal
        }

        // 向后兼容：从ThreadLocal获取（用于Service层等非Controller场景）
        return adminTl.get();
    }

    public static void removeAdmin(){
        adminTl.remove();
    }

    // 清理所有上下文
    public static void clearAll(){
        tl.remove();
        adminTl.remove();
    }
}

