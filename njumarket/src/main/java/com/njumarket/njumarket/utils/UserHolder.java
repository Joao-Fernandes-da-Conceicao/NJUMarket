package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.Admin;

/**
 * 用户上下文工具类
 */
public class UserHolder {
    private static final ThreadLocal<User> tl = new ThreadLocal<>();
    private static final ThreadLocal<Admin> adminTl = new ThreadLocal<>();

    // 用户相关方法
    public static void saveUser(User user){
        tl.set(user);
    }

    public static User getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }

    // 管理员相关方法
    public static void saveAdmin(Admin admin){
        adminTl.set(admin);
    }

    public static Admin getAdmin(){
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
