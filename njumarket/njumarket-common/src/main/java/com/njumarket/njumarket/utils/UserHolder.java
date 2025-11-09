package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.model.IUser;
import com.njumarket.njumarket.model.IAdmin;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户上下文工具类（Common模块）
 * 使用ThreadLocal存储用户和管理员信息
 * 使用接口避免反射调用，提高类型安全性和性能
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
     * 
     * @return 当前用户对象（IUser接口），如果未登录返回null
     */
    public static IUser getUser() {
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

        // 向后兼容：从ThreadLocal获取（用于Service层等非Controller场景）
        Object user = userTl.get();
        return user instanceof IUser ? (IUser) user : null;
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
     * 
     * @return 当前管理员对象（IAdmin接口），如果未登录返回null
     */
    public static IAdmin getAdmin() {
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

        // 向后兼容：从ThreadLocal获取（用于Service层等非Controller场景）
        Object admin = adminTl.get();
        return admin instanceof IAdmin ? (IAdmin) admin : null;
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

