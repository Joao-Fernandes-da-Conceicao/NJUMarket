package com.njumarket.auth.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.auth.dto.UserDTO;
import com.njumarket.auth.dto.PublicUserDTO;
import com.njumarket.auth.dto.UserProfileDTO;
import com.njumarket.auth.dto.PublicUserProfileDTO;
import com.njumarket.auth.dto.UserProfileUpdateDTO;
import com.njumarket.auth.dto.internal.UserProfileInternalDTOConverter;
import com.njumarket.auth.entity.User;
import com.njumarket.auth.entity.UserProfile;
import com.njumarket.auth.repository.UserRepository;
import com.njumarket.auth.repository.UserProfileRepository;
import com.njumarket.auth.service.UserProfileService;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.utils.BusinessValidator;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.njumarket.utils.CacheUtil;
import com.njumarket.njumarket.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final CacheUtil cacheUtil;
    private final UserProfileInternalDTOConverter userProfileInternalDTOConverter;

    @Override
    public Result getUserProfile(String userId) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");

        // 检查是否是查看自己的资料（使用 SecurityUtils）
        Object userObj = SecurityUtils.getCurrentUser();
        User currentUser = userObj instanceof User ? (User) userObj : null;
        boolean isSelf = currentUser != null && currentUser.getUserId().equals(userId);
        
        // ✅ 使用缓存（Cache Aside模式）
        // 缓存完整的 UserProfileDTO，根据是否是自己的资料决定返回完整信息还是公开信息
        String cacheKey = RedisConstants.CACHE_USER_PROFILE_DETAIL_KEY + userId;
        UserProfileDTO profileDTO = cacheUtil.getWithFallback(
            cacheKey,
            RedisConstants.CACHE_USER_PROFILE_TTL * 60, // 转换为秒
            UserProfileDTO.class,
            () -> {
                // 缓存未命中，从数据库加载
                UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("用户档案不存在"));
                return convertToDTO(profile);
            }
        );
        
        if (isSelf) {
            // 查看自己的资料，返回完整信息
            return Result.ok(profileDTO);
        } else {
            // 查看他人资料，返回公开信息（不含敏感数据）
            PublicUserProfileDTO publicDTO = convertToPublicDTO(profileDTO);
            return Result.ok(publicDTO);
        }
    }
    
    @Override
    public Result getPublicUserProfile(String userId) {
        BusinessValidator.requireNotBlank(userId, "用户ID不能为空");

        // ✅ 使用缓存（Cache Aside模式）
        // 优先从缓存获取完整的 UserProfileDTO，然后转换为公开信息
        String cacheKey = RedisConstants.CACHE_USER_PROFILE_DETAIL_KEY + userId;
        UserProfileDTO profileDTO = cacheUtil.getWithFallback(
            cacheKey,
            RedisConstants.CACHE_USER_PROFILE_TTL * 60, // 转换为秒
            UserProfileDTO.class,
            () -> {
                // 缓存未命中，从数据库加载
                UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("用户档案不存在"));
                return convertToDTO(profile);
            }
        );
        
        // 转换为公开信息（不含敏感数据）
        PublicUserProfileDTO publicDTO = convertToPublicDTO(profileDTO);
        return Result.ok(publicDTO);
    }

    @Override
    public Result getCurrentUserProfile() {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
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
        
        // ✅ Cache Aside模式：先更新数据库，再删除缓存
        // 删除用户档案缓存（档案信息变更后需要清除缓存）
        // ⚠️ 注意：只删除自己的缓存，不要误删别人的
        if (cacheUtil != null) {
            String cacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + userId;
            String detailCacheKey = RedisConstants.CACHE_USER_PROFILE_DETAIL_KEY + userId;
            cacheUtil.delete(cacheKey);
            cacheUtil.delete(detailCacheKey);
            log.info("已删除用户档案缓存（Cache Aside模式）: userId={}, reason=档案信息变更, cacheKeys=[{}, {}]", userId, cacheKey, detailCacheKey);
        }
        
        
        return Result.ok(convertToDTO(profile));
    }

    @Override
    public Result updateCurrentUserProfile(UserProfileUpdateDTO updateDTO) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
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

        UserProfile savedProfile = userProfileRepository.save(profile);
        
        
        return Result.ok(convertToDTO(savedProfile));
    }

    @Override
    public Result setAvatarUrl(String userId, String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException("头像 URL 不能为空");
        }
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("用户档案不存在"));

        profile.setAvatar(imageUrl.trim());
        userProfileRepository.save(profile);

        // ✅ Cache Aside：更新 DB 后删除缓存
        evictUserProfileCache(userId, "头像变更");

        log.info("用户头像 URL 已更新: userId={}", userId);
        return Result.ok("头像更新成功", Map.of("imageUrl", imageUrl.trim()));
    }

    @Override
    public Result deleteAvatar(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("用户档案不存在"));

        if (!StringUtils.hasText(profile.getAvatar())) {
            return Result.ok("用户没有头像，无需删除");
        }

        // Auth 服务只负责档案元数据：清空 URL 即可
        // 物理文件由 Image 服务侧管理（新上传时自动覆盖旧文件）
        profile.setAvatar(null);
        userProfileRepository.save(profile);

        // ✅ Cache Aside：更新 DB 后删除缓存
        evictUserProfileCache(userId, "头像删除");

        log.info("用户头像 URL 已清空: userId={}", userId);
        return Result.ok("头像删除成功");
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
     * 从 UserProfileDTO 转换（用于缓存场景）
     */
    private PublicUserProfileDTO convertToPublicDTO(UserProfileDTO profileDTO) {
        if (profileDTO == null) {
            return null;
        }
        
        PublicUserProfileDTO dto = new PublicUserProfileDTO();
        dto.setProfileId(profileDTO.getProfileId());
        dto.setUserId(profileDTO.getUserId());
        dto.setNickname(profileDTO.getNickname());
        dto.setAvatar(profileDTO.getAvatar());
        
        // 转换用户基本信息（不含敏感数据）
        if (profileDTO.getUserInfo() != null) {
            UserDTO userInfo = profileDTO.getUserInfo();
            PublicUserDTO publicUserDTO = new PublicUserDTO();
            publicUserDTO.setUserId(userInfo.getUserId());
            publicUserDTO.setAccountStatus(userInfo.getAccountStatus());
            publicUserDTO.setRegisterTime(userInfo.getRegisterTime());
            publicUserDTO.setNickname(profileDTO.getNickname());
            publicUserDTO.setAvatar(profileDTO.getAvatar());
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
     * 清除用户档案缓存（Cache Aside模式）
     * ⚠️ 注意：只删除指定用户的缓存，不会误删别人的缓存
     * 
     * @param userId 用户ID
     * @param reason 清除原因（用于日志）
     */
    private void evictUserProfileCache(String userId, String reason) {
        if (cacheUtil != null) {
            // 删除批量查询缓存（UserProfileInternalDTO）
            String cacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + userId;
            // 删除单条查询缓存（UserProfileDTO）
            String detailCacheKey = RedisConstants.CACHE_USER_PROFILE_DETAIL_KEY + userId;
            cacheUtil.delete(cacheKey);
            cacheUtil.delete(detailCacheKey);
            log.info("已删除用户档案缓存（Cache Aside模式）: userId={}, reason={}, cacheKeys=[{}, {}]", userId, reason, cacheKey, detailCacheKey);
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

    // ========== 内部方法 ==========

    @Override
    public List<UserProfileInternalDTO> getUserProfilesByIdsInternal(List<String> userIds) {
        List<UserProfileInternalDTO> result = new ArrayList<>();
        List<String> missingUserIds = new ArrayList<>();

        // 先从缓存读取
        for (String userId : userIds) {
            String cacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + userId;
            UserProfileInternalDTO cached = cacheUtil.get(cacheKey, UserProfileInternalDTO.class);
            if (cached != null) {
                result.add(cached);
            } else {
                missingUserIds.add(userId);
            }
        }

        // 缓存缺失的批量查数据库，并回写缓存
        if (!missingUserIds.isEmpty()) {
            List<UserProfile> profiles = userProfileRepository.findByUserIdIn(missingUserIds);
            List<UserProfileInternalDTO> newDtos = userProfileInternalDTOConverter.toUserProfileInternalDTOList(profiles);
            for (UserProfileInternalDTO dto : newDtos) {
                String cacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + dto.getUserId();
                cacheUtil.set(cacheKey, dto, RedisConstants.CACHE_USER_PROFILE_TTL * 60);
            }
            result.addAll(newDtos);
        }

        return result;
    }
}

