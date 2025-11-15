package com.njumarket.auth.dto.internal;

import com.njumarket.auth.entity.User;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户内部 DTO 转换器（Auth Service专用）
 * 用于将 User 实体转换为内部 DTO（用于服务间通信）
 */
@Component
public class UserInternalDTOConverter {
    
    /**
     * 将 User 转换为 UserInternalDTO
     * ✅ 添加accountStatus默认值处理：如果为null或空，默认设置为"ACTIVE"
     * 这样可以避免缓存中存储的旧数据（accountStatus为null）导致的问题
     */
    public UserInternalDTO toInternalDTO(User user) {
        if (user == null) {
            return null;
        }
        
        UserInternalDTO dto = new UserInternalDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setPrimaryPhone(user.getPrimaryPhone());
        // ✅ 添加默认值处理：如果accountStatus为null或空，设置为"ACTIVE"
        String accountStatus = user.getAccountStatus();
        dto.setAccountStatus(accountStatus != null && !accountStatus.trim().isEmpty() 
            ? accountStatus 
            : "ACTIVE");
        dto.setRegisterTime(user.getRegisterTime());
        return dto;
    }
    
    /**
     * 批量转换 User 列表
     */
    public List<UserInternalDTO> toUserInternalDTOList(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
            .map(this::toInternalDTO)
            .collect(Collectors.toList());
    }
}

