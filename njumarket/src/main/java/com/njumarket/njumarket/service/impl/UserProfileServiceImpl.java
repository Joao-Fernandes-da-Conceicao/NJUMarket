package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.UserDTO;
import com.njumarket.njumarket.dto.PublicUserDTO;
import com.njumarket.njumarket.dto.UserProfileDTO;
import com.njumarket.njumarket.dto.PublicUserProfileDTO;
import com.njumarket.njumarket.dto.UserProfileUpdateDTO;
import com.njumarket.njumarket.dto.ImageUploadDTO;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.UserProfile;
import com.njumarket.njumarket.repository.UserRepository;
import com.njumarket.njumarket.repository.UserProfileRepository;
import com.njumarket.njumarket.service.UserProfileService;
import com.njumarket.njumarket.service.ImageService;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.utils.BusinessValidator;
import com.njumarket.njumarket.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户档案服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;

    @Override
    public Result getUserProfile(String userId) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("用户档案不存在"));

        // 检查是否是查看自己的资料
        User currentUser = UserHolder.getUser();
        boolean isSelf = currentUser != null && currentUser.getUserId().equals(userId);
        
        if (isSelf) {
            // 查看自己的资料，返回完整信息
            UserProfileDTO profileDTO = convertToDTO(profile);
            return Result.ok(profileDTO);
        } else {
            // 查看他人资料，返回公开信息（不含敏感数据）
            PublicUserProfileDTO publicDTO = convertToPublicDTO(profile);
            return Result.ok(publicDTO);
        }
    }
    
    @Override
    public Result getPublicUserProfile(String userId) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("用户档案不存在"));

        PublicUserProfileDTO publicDTO = convertToPublicDTO(profile);
        return Result.ok(publicDTO);
    }

    @Override
    public Result getCurrentUserProfile() {
        User currentUser = BusinessValidator.requireLogin();
        return getUserProfile(currentUser.getUserId());
    }

    @Override
    public Result updateUserProfile(String userId, UserProfileUpdateDTO updateDTO) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("用户档案不存在"));
        
        // 更新档案信息
        if (updateDTO.getNickname() != null && !updateDTO.getNickname().trim().isEmpty()) {
            String nickname = updateDTO.getNickname().trim();
            if (nickname.length() > 50) {
                throw new BusinessException("昵称长度不能超过50个字符");
            }
            profile.setNickname(nickname);
        }

        if (updateDTO.getAvatar() != null && !updateDTO.getAvatar().trim().isEmpty()) {
            profile.setAvatar(updateDTO.getAvatar().trim());
        }

        userProfileRepository.save(profile);
        return Result.ok(convertToDTO(profile));
    }

    @Override
    public Result updateCurrentUserProfile(UserProfileUpdateDTO updateDTO) {
        User currentUser = BusinessValidator.requireLogin();
        return updateUserProfile(currentUser.getUserId(), updateDTO);
    }

    @Override
    public Result createUserProfile(String userId, String nickname) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");

        // 检查用户是否存在
        BusinessValidator.requireUser(userId, userRepository);

        // 检查是否已有档案
        if (userProfileRepository.existsByUserId(userId)) {
            throw new BusinessException("用户档案已存在");
        }

        // 创建新档案
        UserProfile profile = new UserProfile();
        profile.setProfileId(generateProfileId());
        profile.setUserId(userId);
        profile.setNickname(nickname != null ? nickname : "用户" + userId.substring(5, 10));
        profile.setCreditScore(100);
        profile.setBuyerRating(5.0);
        profile.setSellerRating(5.0);
        profile.setTotalSales(0);
        profile.setTotalPurchases(0);
        profile.setVipLevel("NORMAL");

        UserProfile savedProfile = userProfileRepository.save(profile);
        return Result.ok(convertToDTO(savedProfile));
    }

    @Override
    public Result uploadAvatar(String userId, MultipartFile file) {
        // 1. 验证用户是否存在
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("用户档案不存在"));
        
        // 2. 上传新头像（ImageService会自动删除旧头像）
        ImageUploadDTO uploadResult = imageService.uploadAvatar(userId, file);
        
        if (!uploadResult.isSuccess()) {
            throw new BusinessException(uploadResult.getMessage());
        }
        
        // 3. 更新用户档案中的头像URL
        profile.setAvatar(uploadResult.getImageUrl());
        userProfileRepository.save(profile);
        
        return Result.ok("头像上传成功", uploadResult);
    }

    @Override
    public Result deleteAvatar(String userId) {
        // 1. 验证用户是否存在
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("用户档案不存在"));
        
        String currentAvatarUrl = profile.getAvatar();
        
        // 2. 如果没有头像，直接返回成功
        if (currentAvatarUrl == null || currentAvatarUrl.trim().isEmpty()) {
            return Result.ok("用户没有头像，无需删除");
        }
        
        // 3. 删除头像文件
        boolean deleted = imageService.deleteAvatarByUrl(currentAvatarUrl);
        if (!deleted) {
            log.warn("头像文件删除失败: userId={}, avatarUrl={}", userId, currentAvatarUrl);
        }
        
        // 4. 清空用户档案中的头像URL
        profile.setAvatar(null);
        userProfileRepository.save(profile);
        
        return Result.ok("头像删除成功");
    }

    @Override
    public Result updateUserRating(String userId, Double rating, String role) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");
        if (rating == null || rating < 0 || rating > 5) {
            throw new BusinessException("评分必须在0-5之间");
        }
        if (!"buyer".equals(role) && !"seller".equals(role)) {
            throw new BusinessException("角色类型错误");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("用户档案不存在"));

        if ("buyer".equals(role)) {
            profile.setBuyerRating(rating);
        } else {
            profile.setSellerRating(rating);
        }

        userProfileRepository.save(profile);
        return Result.ok("评分更新成功");
    }

    @Override
    public Result updateCreditScore(String userId, Integer scoreChange, String reason) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");
        if (scoreChange == null || scoreChange == 0) {
            throw new BusinessException("分数变化不能为空或0");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("用户档案不存在"));

        int newScore = profile.getCreditScore() + scoreChange;
        
        // 信用分不能低于0
        if (newScore < 0) {
            newScore = 0;
        }
        
        profile.setCreditScore(newScore);
        userProfileRepository.save(profile);
        
        return Result.ok("信用分更新成功，当前分数：" + newScore);
    }

    @Override
    public Result updateTradeStatistics(String userId, String type, Integer count) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");
        if (!"sale".equals(type) && !"purchase".equals(type)) {
            throw new BusinessException("交易类型错误");
        }
        if (count == null || count <= 0) {
            throw new BusinessException("数量必须大于0");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("用户档案不存在"));

        if ("sale".equals(type)) {
            profile.setTotalSales(profile.getTotalSales() + count);
        } else {
            profile.setTotalPurchases(profile.getTotalPurchases() + count);
        }

        userProfileRepository.save(profile);
        
        // 检查是否需要升级VIP
        checkAndUpgradeVip(profile);
        
        return Result.ok("交易统计更新成功");
    }

    @Override
    public Result upgradeVipLevel(String userId) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("用户档案不存在"));

        String newLevel = calculateVipLevel(profile);
        
        if (!newLevel.equals(profile.getVipLevel())) {
            profile.setVipLevel(newLevel);
            userProfileRepository.save(profile);
            return Result.ok("VIP等级升级为：" + newLevel);
        }
        
        return Result.ok("当前已是最高等级：" + profile.getVipLevel());
    }

    @Override
    public Result getUserRankings(String type, Integer limit) {
        if (!"seller".equals(type) && !"buyer".equals(type)) {
            throw new BusinessException("排行榜类型错误");
        }
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<UserProfile> profiles;
        if ("seller".equals(type)) {
            profiles = userProfileRepository.findTopSellersByRating();
        } else {
            profiles = userProfileRepository.findTopBuyersByRating();
        }

        List<UserProfileDTO> rankings = profiles.stream()
            .limit(limit)
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        return Result.ok(rankings);
    }

    @Override
    public Result searchUserProfiles(String keyword, Integer page, Integer size) {
        BusinessValidator.requireNotBlank(keyword, "搜索关键词不能为空");
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 10;
        }

        List<UserProfile> profiles = userProfileRepository.findByNicknameContaining(keyword.trim());
        
        // 简单分页处理
        int start = page * size;
        int end = Math.min(start + size, profiles.size());
        
        if (start >= profiles.size()) {
            return Result.ok(Collections.emptyList());
        }
        
        List<UserProfileDTO> result = profiles.subList(start, end).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("profiles", result);
        response.put("total", profiles.size());
        response.put("page", page);
        response.put("size", size);

        return Result.ok(response);
    }

    @Override
    public Result getVipLevelStatistics() {
        List<Object[]> statistics = userProfileRepository.countByVipLevel();
        Map<String, Long> result = new HashMap<>();
        
        for (Object[] stat : statistics) {
            String level = (String) stat[0];
            Long count = (Long) stat[1];
            result.put(level, count);
        }
        
        return Result.ok(result);
    }

    @Override
    public boolean hasUserProfile(String userId) {
        return userProfileRepository.existsByUserId(userId);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 转换为DTO
     */
    private UserProfileDTO convertToDTO(UserProfile profile) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setProfileId(profile.getProfileId());
        dto.setUserId(profile.getUserId());
        dto.setNickname(profile.getNickname());
        dto.setAvatar(profile.getAvatar());
        dto.setCreditScore(profile.getCreditScore());
        dto.setBuyerRating(profile.getBuyerRating());
        dto.setSellerRating(profile.getSellerRating());
        dto.setTotalSales(profile.getTotalSales());
        dto.setTotalPurchases(profile.getTotalPurchases());
        dto.setVipLevel(profile.getVipLevel());
        
        // 获取用户基本信息
        Optional<User> userOpt = userRepository.findById(profile.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(user.getUserId());
            userDTO.setPrimaryPhone(user.getPrimaryPhone());
            userDTO.setAccountStatus(user.getAccountStatus());
            userDTO.setRegisterTime(user.getRegisterTime());
            dto.setUserInfo(userDTO);
        }
        
        return dto;
    }
    
    /**
     * 转换为公开DTO（不包含敏感信息）
     */
    private PublicUserProfileDTO convertToPublicDTO(UserProfile profile) {
        PublicUserProfileDTO dto = new PublicUserProfileDTO();
        dto.setProfileId(profile.getProfileId());
        dto.setUserId(profile.getUserId());
        dto.setNickname(profile.getNickname());
        dto.setAvatar(profile.getAvatar());
        dto.setCreditScore(profile.getCreditScore());
        dto.setBuyerRating(profile.getBuyerRating());
        dto.setSellerRating(profile.getSellerRating());
        dto.setTotalSales(profile.getTotalSales());
        dto.setTotalPurchases(profile.getTotalPurchases());
        dto.setVipLevel(profile.getVipLevel());
        
        // 获取用户基本信息（不含敏感数据）
        Optional<User> userOpt = userRepository.findById(profile.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            PublicUserDTO publicUserDTO = new PublicUserDTO();
            publicUserDTO.setUserId(user.getUserId());
            publicUserDTO.setAccountStatus(user.getAccountStatus());
            publicUserDTO.setRegisterTime(user.getRegisterTime());
            publicUserDTO.setNickname(profile.getNickname());
            publicUserDTO.setAvatar(profile.getAvatar());
            publicUserDTO.setCreditScore(profile.getCreditScore());
            publicUserDTO.setBuyerRating(profile.getBuyerRating());
            publicUserDTO.setSellerRating(profile.getSellerRating());
            publicUserDTO.setVipLevel(profile.getVipLevel());
            dto.setUserInfo(publicUserDTO);
        }
        
        return dto;
    }

    /**
     * 生成档案ID
     */
    private String generateProfileId() {
        return "PROFILE_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
    }

    /**
     * 计算VIP等级
     */
    private String calculateVipLevel(UserProfile profile) {
        int totalTrades = profile.getTotalSales() + profile.getTotalPurchases();
        double avgRating = (profile.getBuyerRating() + profile.getSellerRating()) / 2;
        int creditScore = profile.getCreditScore();

        if (totalTrades >= 100 && avgRating >= 4.8 && creditScore >= 150) {
            return "PLATINUM";
        } else if (totalTrades >= 50 && avgRating >= 4.5 && creditScore >= 120) {
            return "GOLD";
        } else if (totalTrades >= 20 && avgRating >= 4.0 && creditScore >= 100) {
            return "SILVER";
        } else if (totalTrades >= 5 && avgRating >= 3.5 && creditScore >= 80) {
            return "BRONZE";
        } else {
            return "NORMAL";
        }
    }

    /**
     * 检查并升级VIP
     */
    private void checkAndUpgradeVip(UserProfile profile) {
        String newLevel = calculateVipLevel(profile);
        if (!newLevel.equals(profile.getVipLevel())) {
            profile.setVipLevel(newLevel);
            userProfileRepository.save(profile);
            log.info("用户VIP等级自动升级: userId={}, newLevel={}", profile.getUserId(), newLevel);
        }
    }
    
    // ✅ v1.3.x: 订单提醒相关方法实现（向后兼容）
    @Override
    public java.util.Map<String, Boolean> getOrderReminderStatus(String userId) {
        java.util.Map<String, Boolean> status = new java.util.HashMap<>();
        status.put("sellerOrderHasNew", false);
        status.put("buyerOrderHasNew", false);
        
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isPresent()) {
                UserProfile profile = profileOpt.get();
                // ✅ 兼容性处理：如果字段为null（旧版本数据），返回false
                status.put("sellerOrderHasNew", 
                    profile.getSellerOrderHasNew() != null && profile.getSellerOrderHasNew());
                status.put("buyerOrderHasNew", 
                    profile.getBuyerOrderHasNew() != null && profile.getBuyerOrderHasNew());
            }
        } catch (Exception e) {
            // ✅ 兼容性处理：如果字段不存在（旧版本数据库），捕获异常并返回默认值
            log.debug("获取订单提醒状态失败（可能是旧版本数据库）: userId={}, error={}", userId, e.getMessage());
        }
        
        return status;
    }
    
    @Override
    public void setOrderReminderStatus(String userId, String role, boolean hasNew) {
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                log.warn("用户档案不存在，无法设置订单提醒状态: userId={}", userId);
                return;
            }
            
            UserProfile profile = profileOpt.get();
            if ("SELLER".equals(role)) {
                profile.setSellerOrderHasNew(hasNew);
            } else if ("BUYER".equals(role)) {
                profile.setBuyerOrderHasNew(hasNew);
            } else {
                log.warn("无效的角色类型: role={}", role);
                return;
            }
            
            userProfileRepository.save(profile);
            log.debug("订单提醒状态已更新: userId={}, role={}, hasNew={}", userId, role, hasNew);
        } catch (Exception e) {
            // ✅ 兼容性处理：如果字段不存在（旧版本数据库），记录警告但不抛出异常
            log.warn("设置订单提醒状态失败（可能是旧版本数据库）: userId={}, role={}, error={}", 
                userId, role, e.getMessage());
        }
    }
    
    @Override
    public void clearOrderReminderStatus(String userId, String role) {
        setOrderReminderStatus(userId, role, false);
    }
}
