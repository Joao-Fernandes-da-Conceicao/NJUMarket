package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.exception.BusinessException;

/**
 * 账号状态验证工具类
 * 统一处理用户和管理员账号状态检查
 * 
 * 设计原则：
 * 1. 统一账号状态检查逻辑
 * 2. 提供友好的错误提示信息
 * 3. 支持在Filter和Service层使用
 */
public class AccountStatusValidator {

    /**
     * 检查用户账号状态
     * 如果账号状态为null、空字符串或非ACTIVE，抛出BusinessException
     * 
     * @param user 用户对象
     * @throws BusinessException 如果账号状态异常
     */
    public static void validateUserAccountStatus(User user) {
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        String accountStatus = user.getAccountStatus();
        if (accountStatus == null || accountStatus.trim().isEmpty()) {
            throw new BusinessException("账户状态异常，请联系管理员");
        }
        
        if (!"ACTIVE".equals(accountStatus)) {
            String statusMessage = getUserAccountStatusMessage(accountStatus);
            throw new BusinessException(statusMessage);
        }
    }

    /**
     * 检查管理员账号状态
     * 如果账号状态为null、空字符串或非ACTIVE，抛出BusinessException
     * 
     * @param admin 管理员对象
     * @throws BusinessException 如果账号状态异常
     */
    public static void validateAdminAccountStatus(Admin admin) {
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        
        String accountStatus = admin.getAccountStatus();
        if (accountStatus == null || accountStatus.trim().isEmpty()) {
            throw new BusinessException("账户状态异常，请联系系统管理员");
        }
        
        if (!"ACTIVE".equals(accountStatus)) {
            String statusMessage = getAdminAccountStatusMessage(accountStatus);
            throw new BusinessException(statusMessage);
        }
    }

    /**
     * 根据用户账号状态获取友好的提示信息
     * 
     * @param accountStatus 账号状态
     * @return 友好的提示信息
     */
    public static String getUserAccountStatusMessage(String accountStatus) {
        if (accountStatus == null) {
            return "账户状态异常，请联系管理员";
        }
        
        return switch (accountStatus) {
            case "SUSPENDED" -> "账户已被暂停，请联系管理员了解详情";
            case "BANNED" -> "账户已被封禁，如有疑问请联系管理员";
            case "DELETED" -> "账户已被删除，无法使用";
            default -> "账户状态异常，请联系管理员";
        };
    }

    /**
     * 根据管理员账号状态获取友好的提示信息
     * 
     * @param accountStatus 账号状态
     * @return 友好的提示信息
     */
    public static String getAdminAccountStatusMessage(String accountStatus) {
        if (accountStatus == null) {
            return "账户状态异常，请联系系统管理员";
        }
        
        return switch (accountStatus) {
            case "SUSPENDED" -> "账户已被暂停，请联系系统管理员了解详情";
            case "BANNED" -> "账户已被封禁，如有疑问请联系系统管理员";
            case "DELETED" -> "账户已被删除，无法使用";
            default -> "账户状态异常，请联系系统管理员";
        };
    }
}

