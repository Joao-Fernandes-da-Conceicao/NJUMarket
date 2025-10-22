package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.UserDTO;
import com.njumarket.njumarket.dto.UserProfileDTO;
import com.njumarket.njumarket.dto.UserProfileUpdateDTO;
import com.njumarket.njumarket.dto.ImageUploadDTO;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.UserProfile;
import com.njumarket.njumarket.repository.UserRepository;
import com.njumarket.njumarket.repository.UserProfileRepository;
import com.njumarket.njumarket.service.UserProfileService;
import com.njumarket.njumarket.service.ImageService;
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
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail("用户ID不能为空");
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return Result.fail("用户档案不存在");
        }

        UserProfileDTO profileDTO = convertToDTO(profileOpt.get());
        return Result.ok(profileDTO);
    }

    @Override
    public Result getCurrentUserProfile() {
        User currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("用户未登录");
        }

        return getUserProfile(currentUser.getUserId());
    }

    @Override
    public Result updateUserProfile(String userId, UserProfileUpdateDTO updateDTO) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail("用户ID不能为空");
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return Result.fail("用户档案不存在");
        }

        UserProfile profile = profileOpt.get();
        
        // 更新档案信息
        if (updateDTO.getNickname() != null && !updateDTO.getNickname().trim().isEmpty()) {
            String nickname = updateDTO.getNickname().trim();
            if (nickname.length() > 50) {
                return Result.fail("昵称长度不能超过50个字符");
            }
            profile.setNickname(nickname);
        }

        if (updateDTO.getAvatar() != null && !updateDTO.getAvatar().trim().isEmpty()) {
            profile.setAvatar(updateDTO.getAvatar().trim());
        }

        try {
            userProfileRepository.save(profile);
            log.info("用户档案更新成功: userId={}", userId);
            return Result.ok(convertToDTO(profile));
        } catch (Exception e) {
            log.error("用户档案更新失败: userId={}, error={}", userId, e.getMessage());
            return Result.fail("档案更新失败");
        }
    }

    @Override
    public Result updateCurrentUserProfile(UserProfileUpdateDTO updateDTO) {
        User currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("用户未登录");
        }

        return updateUserProfile(currentUser.getUserId(), updateDTO);
    }

    @Override
    public Result createUserProfile(String userId, String nickname) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail("用户ID不能为空");
        }

        // 检查用户是否存在
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.fail("用户不存在");
        }

        // 检查是否已有档案
        if (userProfileRepository.existsByUserId(userId)) {
            return Result.fail("用户档案已存在");
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

        try {
            UserProfile savedProfile = userProfileRepository.save(profile);
            log.info("用户档案创建成功: userId={}, profileId={}", userId, savedProfile.getProfileId());
            return Result.ok(convertToDTO(savedProfile));
        } catch (Exception e) {
            log.error("用户档案创建失败: userId={}, error={}", userId, e.getMessage());
            return Result.fail("档案创建失败");
        }
    }

    @Override
    public Result uploadAvatar(String userId, MultipartFile file) {
        try {
            // 1. 验证用户是否存在
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty()) {
                return Result.fail("用户档案不存在");
            }
            
            UserProfile profile = profileOpt.get();
            String oldAvatarUrl = profile.getAvatar();
            
            // 2. 如果存在旧头像，先删除旧头像文件
            if (oldAvatarUrl != null && !oldAvatarUrl.trim().isEmpty()) {
                try {
                    boolean deleted = imageService.deleteAvatarByUrl(oldAvatarUrl);
                    if (deleted) {
                        log.info("旧头像删除成功: userId={}, oldAvatarUrl={}", userId, oldAvatarUrl);
                    } else {
                        log.warn("旧头像删除失败: userId={}, oldAvatarUrl={}", userId, oldAvatarUrl);
                    }
                } catch (Exception e) {
                    log.error("删除旧头像时发生异常: userId={}, oldAvatarUrl={}, error={}", 
                        userId, oldAvatarUrl, e.getMessage());
                    // 继续执行，不因为删除旧头像失败而中断上传流程
                }
            }
            
            // 3. 上传新头像
            ImageUploadDTO uploadResult = imageService.uploadAvatar(userId, file);
            
            // 4. 更新用户档案中的头像URL
            profile.setAvatar(uploadResult.getImageUrl());
            userProfileRepository.save(profile);
            
            log.info("用户头像上传成功: userId={}, oldAvatarUrl={}, newAvatarUrl={}", 
                userId, oldAvatarUrl, uploadResult.getImageUrl());
            return Result.ok(uploadResult);
            
        } catch (Exception e) {
            log.error("用户头像上传失败: userId={}, error={}", userId, e.getMessage());
            return Result.fail("头像上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateUserRating(String userId, Double rating, String role) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail("用户ID不能为空");
        }
        if (rating == null || rating < 0 || rating > 5) {
            return Result.fail("评分必须在0-5之间");
        }
        if (!"buyer".equals(role) && !"seller".equals(role)) {
            return Result.fail("角色类型错误");
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return Result.fail("用户档案不存在");
        }

        UserProfile profile = profileOpt.get();
        if ("buyer".equals(role)) {
            profile.setBuyerRating(rating);
        } else {
            profile.setSellerRating(rating);
        }

        try {
            userProfileRepository.save(profile);
            log.info("用户评分更新成功: userId={}, role={}, rating={}", userId, role, rating);
            return Result.ok("评分更新成功");
        } catch (Exception e) {
            log.error("用户评分更新失败: userId={}, error={}", userId, e.getMessage());
            return Result.fail("评分更新失败");
        }
    }

    @Override
    public Result updateCreditScore(String userId, Integer scoreChange, String reason) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail("用户ID不能为空");
        }
        if (scoreChange == null || scoreChange == 0) {
            return Result.fail("分数变化不能为空或0");
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return Result.fail("用户档案不存在");
        }

        UserProfile profile = profileOpt.get();
        int newScore = profile.getCreditScore() + scoreChange;
        
        // 信用分不能低于0
        if (newScore < 0) {
            newScore = 0;
        }
        
        profile.setCreditScore(newScore);

        try {
            userProfileRepository.save(profile);
            log.info("用户信用分更新成功: userId={}, change={}, newScore={}, reason={}", 
                userId, scoreChange, newScore, reason);
            return Result.ok("信用分更新成功，当前分数：" + newScore);
        } catch (Exception e) {
            log.error("用户信用分更新失败: userId={}, error={}", userId, e.getMessage());
            return Result.fail("信用分更新失败");
        }
    }

    @Override
    public Result updateTradeStatistics(String userId, String type, Integer count) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail("用户ID不能为空");
        }
        if (!"sale".equals(type) && !"purchase".equals(type)) {
            return Result.fail("交易类型错误");
        }
        if (count == null || count <= 0) {
            return Result.fail("数量必须大于0");
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return Result.fail("用户档案不存在");
        }

        UserProfile profile = profileOpt.get();
        if ("sale".equals(type)) {
            profile.setTotalSales(profile.getTotalSales() + count);
        } else {
            profile.setTotalPurchases(profile.getTotalPurchases() + count);
        }

        try {
            userProfileRepository.save(profile);
            log.info("用户交易统计更新成功: userId={}, type={}, count={}", userId, type, count);
            
            // 检查是否需要升级VIP
            checkAndUpgradeVip(profile);
            
            return Result.ok("交易统计更新成功");
        } catch (Exception e) {
            log.error("用户交易统计更新失败: userId={}, error={}", userId, e.getMessage());
            return Result.fail("交易统计更新失败");
        }
    }

    @Override
    public Result upgradeVipLevel(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail("用户ID不能为空");
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return Result.fail("用户档案不存在");
        }

        UserProfile profile = profileOpt.get();
        String newLevel = calculateVipLevel(profile);
        
        if (!newLevel.equals(profile.getVipLevel())) {
            profile.setVipLevel(newLevel);
            try {
                userProfileRepository.save(profile);
                log.info("用户VIP等级升级成功: userId={}, newLevel={}", userId, newLevel);
                return Result.ok("VIP等级升级为：" + newLevel);
            } catch (Exception e) {
                log.error("用户VIP等级升级失败: userId={}, error={}", userId, e.getMessage());
                return Result.fail("VIP等级升级失败");
            }
        }
        
        return Result.ok("当前已是最高等级：" + profile.getVipLevel());
    }

    @Override
    public Result getUserRankings(String type, Integer limit) {
        if (!"seller".equals(type) && !"buyer".equals(type)) {
            return Result.fail("排行榜类型错误");
        }
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        try {
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
        } catch (Exception e) {
            log.error("获取用户排行榜失败: type={}, error={}", type, e.getMessage());
            return Result.fail("获取排行榜失败");
        }
    }

    @Override
    public Result searchUserProfiles(String keyword, Integer page, Integer size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.fail("搜索关键词不能为空");
        }
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 10;
        }

        try {
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
        } catch (Exception e) {
            log.error("搜索用户档案失败: keyword={}, error={}", keyword, e.getMessage());
            return Result.fail("搜索失败");
        }
    }

    @Override
    public Result getVipLevelStatistics() {
        try {
            List<Object[]> statistics = userProfileRepository.countByVipLevel();
            Map<String, Long> result = new HashMap<>();
            
            for (Object[] stat : statistics) {
                String level = (String) stat[0];
                Long count = (Long) stat[1];
                result.put(level, count);
            }
            
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取VIP等级统计失败: error={}", e.getMessage());
            return Result.fail("获取统计信息失败");
        }
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
}
