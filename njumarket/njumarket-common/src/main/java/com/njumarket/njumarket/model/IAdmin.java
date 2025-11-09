package com.njumarket.njumarket.model;

/**
 * 管理员接口（Common模块）
 * 定义管理员实体类必须实现的方法，用于SecurityUtils和UserHolder
 * 避免使用反射调用方法
 */
public interface IAdmin {
    
    /**
     * 获取管理员ID
     * @return 管理员ID
     */
    String getAdminId();
    
    /**
     * 检查是否为系统管理员
     * @return true表示是系统管理员，false表示不是
     */
    Boolean isSystemAdmin();
    
    /**
     * 检查是否拥有指定权限
     * @param permission 权限名称
     * @return true表示拥有权限，false表示没有
     */
    Boolean hasPermission(String permission);
}

