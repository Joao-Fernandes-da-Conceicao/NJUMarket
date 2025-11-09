package com.njumarket.njumarket.model;

/**
 * 用户接口（Common模块）
 * 定义用户实体类必须实现的方法，用于SecurityUtils和UserHolder
 * 避免使用反射调用方法
 */
public interface IUser {
    
    /**
     * 获取用户ID
     * @return 用户ID
     */
    String getUserId();
    
    /**
     * 获取账户状态
     * @return 账户状态（ACTIVE, SUSPENDED, BANNED等）
     */
    String getAccountStatus();
}

