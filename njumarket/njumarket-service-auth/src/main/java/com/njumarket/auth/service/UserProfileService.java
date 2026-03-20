package com.njumarket.auth.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.auth.dto.UserProfileUpdateDTO;

import java.util.List;

/**
 * 用户档案服务接口
 */
public interface UserProfileService {
    
    /**
     * 获取用户档案（根据当前登录用户决定返回完整或公开信息）
     */
    Result getUserProfile(String userId);
    
    /**
     * 获取公开用户档案（仅包含公开信息，不含敏感数据）
     */
    Result getPublicUserProfile(String userId);
    
    /**
     * 获取当前用户档案
     */
    Result getCurrentUserProfile();
    
    /**
     * 更新用户档案
     */
    Result updateUserProfile(String userId, UserProfileUpdateDTO updateDTO);
    
    /**
     * 更新当前用户档案
     */
    Result updateCurrentUserProfile(UserProfileUpdateDTO updateDTO);
    
    /**
     * 创建用户档案
     */
    Result createUserProfile(String userId, String nickname);
    
    /**
     * 设置头像 URL（前端先调 Image 服务上传，拿到 imageUrl 后调此方法写入档案）
     */
    Result setAvatarUrl(String userId, String imageUrl);

    /**
     * 删除头像（清空档案中的头像 URL；物理文件由 Image 服务侧管理）
     */
    Result deleteAvatar(String userId);
    
    /**
     * 搜索用户档案
     */
    Result searchUserProfiles(String keyword, Integer page, Integer size);
    
    /**
     * 检查用户是否有档案
     */
    boolean hasUserProfile(String userId);
    
    // ✅ v1.3.x: 订单提醒相关方法（向后兼容，不影响现有功能）
    /**
     * 获取订单提醒状态
     * @param userId 用户ID
     * @return Map包含 sellerOrderHasNew 和 buyerOrderHasNew（如果字段不存在则返回false）
     */
    java.util.Map<String, Boolean> getOrderReminderStatus(String userId);
    
    /**
     * 设置订单提醒状态
     * @param userId 用户ID
     * @param role 角色（"SELLER" 或 "BUYER"）
     * @param hasNew 是否有新变化
     */
    void setOrderReminderStatus(String userId, String role, boolean hasNew);
    
    /**
     * 清除订单提醒状态
     * @param userId 用户ID
     * @param role 角色（"SELLER" 或 "BUYER"）
     */
    void clearOrderReminderStatus(String userId, String role);

    // ========== 内部方法 ==========

    /**
     * 批量查询用户档案（带缓存，供服务间调用）
     */
    List<UserProfileInternalDTO> getUserProfilesByIdsInternal(List<String> userIds);
}

