package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.Admin;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
     */
    public static User getUser(){
        // 优先从Spring Security SecurityContext获取
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            }
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
     */
    public static Admin getAdmin(){
        // 优先从Spring Security SecurityContext获取
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Admin) {
                return (Admin) principal;
            }
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
