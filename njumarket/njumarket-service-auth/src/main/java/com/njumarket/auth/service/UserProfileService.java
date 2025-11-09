package com.njumarket.auth.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.auth.dto.UserProfileDTO;
import com.njumarket.auth.dto.UserProfileUpdateDTO;
import org.springframework.web.multipart.MultipartFile;

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
     * 上传头像
     */
    Result uploadAvatar(String userId, MultipartFile file);
    
    /**
     * 删除头像
     */
    Result deleteAvatar(String userId);
    
    /**
     * 更新用户评分
     */
    Result updateUserRating(String userId, Double rating, String role);
    
    /**
     * 更新信用分
     */
    Result updateCreditScore(String userId, Integer scoreChange, String reason);
    
    /**
     * 更新交易统计
     */
    Result updateTradeStatistics(String userId, String type, Integer count);
    
    /**
     * 升级VIP等级
     */
    Result upgradeVipLevel(String userId);
    
    /**
     * 获取用户排行榜
     */
    Result getUserRankings(String type, Integer limit);
    
    /**
     * 搜索用户档案
     */
    Result searchUserProfiles(String keyword, Integer page, Integer size);
    
    /**
     * 获取VIP等级统计
     */
    Result getVipLevelStatistics();
    
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
}

