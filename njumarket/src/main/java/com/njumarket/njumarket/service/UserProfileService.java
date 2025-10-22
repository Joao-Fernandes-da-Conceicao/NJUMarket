package com.njumarket.njumarket.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.UserProfileDTO;
import com.njumarket.njumarket.dto.UserProfileUpdateDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户档案服务接口
 */
public interface UserProfileService {
    
    /**
     * 获取用户档案
     */
    Result getUserProfile(String userId);
    
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
}
