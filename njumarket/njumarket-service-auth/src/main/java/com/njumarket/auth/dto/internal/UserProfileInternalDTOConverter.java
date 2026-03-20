package com.njumarket.auth.dto.internal;

import com.njumarket.auth.entity.UserProfile;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户档案内部 DTO 转换器（Auth Service专用）
 * 用于将 UserProfile 实体转换为内部 DTO（用于服务间通信）
 */
@Component
public class UserProfileInternalDTOConverter {
    
    /**
     * 将 UserProfile 转换为 UserProfileInternalDTO
     */
    public UserProfileInternalDTO toInternalDTO(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        
        UserProfileInternalDTO dto = new UserProfileInternalDTO();
        dto.setProfileId(profile.getProfileId());
        dto.setUserId(profile.getUserId());
        dto.setNickname(profile.getNickname());
        dto.setAvatar(profile.getAvatar());
        // location 和 bio 字段在 UserProfile 实体中不存在，设置为 null
        dto.setLocation(null);
        dto.setBio(null);
        dto.setSellerOrderHasNew(profile.getSellerOrderHasNew());
        dto.setBuyerOrderHasNew(profile.getBuyerOrderHasNew());
        return dto;
    }
    
    /**
     * 批量转换 UserProfile 列表
     */
    public List<UserProfileInternalDTO> toUserProfileInternalDTOList(List<UserProfile> profiles) {
        if (profiles == null) {
            return null;
        }
        return profiles.stream()
            .map(this::toInternalDTO)
            .collect(Collectors.toList());
    }
}

