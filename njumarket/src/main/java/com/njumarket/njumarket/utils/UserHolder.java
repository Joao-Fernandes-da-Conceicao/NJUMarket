package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.entity.User;

/**
 * 用户上下文工具类
 */
public class UserHolder {
    private static final ThreadLocal<User> tl = new ThreadLocal<>();

    public static void saveUser(User user){
        tl.set(user);
    }

    public static User getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
