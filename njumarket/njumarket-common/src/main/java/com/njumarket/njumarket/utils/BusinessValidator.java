package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.exception.BusinessException;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.StringUtils;

/**
 * 业务校验工具类（Common模块）
 * 统一封装常见业务校验逻辑，减少重复代码
 * 使用反射避免编译时依赖实体类
 */
public class BusinessValidator {
    
    /**
     * 检查用户是否登录
     * @return 当前登录用户对象
     * @throws BusinessException 如果用户未登录
     */
    public static Object requireLogin() {
        Object user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException("用户未登录");
        }
        return user;
    }
    
    /**
     * 检查字符串非空
     * @param value 要检查的值
     * @param message 错误消息
     * @throws BusinessException 如果值为空
     */
    public static void requireNotBlank(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 检查用户是否存在（使用反射）
     * @param userId 用户ID
     * @param repository 用户Repository
     * @return 用户对象
     * @throws BusinessException 如果用户不存在
     */
    public static Object requireUser(String userId, CrudRepository<?, String> repository) {
        return repository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
    }
    
    /**
     * 检查用户账户状态（使用反射）
     * @param user 用户对象
     * @throws BusinessException 如果账户未激活
     */
    public static void requireActiveUser(Object user) {
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        try {
            String accountStatus = (String) user.getClass().getMethod("getAccountStatus").invoke(user);
            if (!"ACTIVE".equals(accountStatus)) {
                throw new BusinessException("用户账户未激活");
            }
        } catch (java.lang.reflect.InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            if (e.getCause() instanceof BusinessException) {
                throw (BusinessException) e.getCause();
            }
            throw new BusinessException("无法检查用户账户状态");
        }
    }
    
    /**
     * 检查管理员是否存在（使用反射）
     * @param adminId 管理员ID
     * @param repository 管理员Repository
     * @return 管理员对象
     * @throws BusinessException 如果管理员不存在
     */
    public static Object requireAdmin(String adminId, CrudRepository<?, String> repository) {
        return repository.findById(adminId)
            .orElseThrow(() -> new BusinessException("管理员不存在"));
    }
    
    /**
     * 检查管理员账户状态（使用反射）
     * @param admin 管理员对象
     * @throws BusinessException 如果账户未激活
     */
    public static void requireActiveAdmin(Object admin) {
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        try {
            String accountStatus = (String) admin.getClass().getMethod("getAccountStatus").invoke(admin);
            if (!"ACTIVE".equals(accountStatus)) {
                throw new BusinessException("管理员账户未激活");
            }
        } catch (java.lang.reflect.InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            if (e.getCause() instanceof BusinessException) {
                throw (BusinessException) e.getCause();
            }
            throw new BusinessException("无法检查管理员账户状态");
        }
    }
}

