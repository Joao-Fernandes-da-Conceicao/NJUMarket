package com.njumarket.auth.utils;

import com.njumarket.auth.entity.User;
import com.njumarket.njumarket.exception.BusinessException;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.StringUtils;

/**
 * 业务校验工具类
 * 统一封装常见业务校验逻辑，减少重复代码
 * 注意：此工具类只包含通用的和安全相关的校验方法
 * 特定业务实体的校验方法应由各自的服务提供
 */
public class BusinessValidator {
    
    /**
     * 检查用户是否登录
     * @return 当前登录用户
     * @throws BusinessException 如果用户未登录
     */
    public static User requireLogin() {
        User user = UserHolder.getUser();
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
     * 检查用户是否存在
     * @param userId 用户ID
     * @param repository 用户Repository
     * @return 用户对象
     * @throws BusinessException 如果用户不存在
     */
    public static User requireUser(String userId, CrudRepository<User, String> repository) {
        return repository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
    }
    
    /**
     * 检查用户账户状态
     * @param user 用户对象
     * @throws BusinessException 如果账户未激活
     */
    public static void requireActiveUser(User user) {
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new BusinessException("用户账户未激活");
        }
    }
    
}

