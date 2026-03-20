package com.njumarket.auth.dto.internal;

import com.njumarket.auth.entity.UserAddress;
import com.njumarket.njumarket.dto.internal.AddressInternalDTO;
import org.springframework.stereotype.Component;

/**
 * 地址内部 DTO 转换器（Auth Service专用）
 * 用于将 UserAddress 实体转换为内部 DTO（用于服务间通信）
 */
@Component
public class AddressInternalDTOConverter {

    public AddressInternalDTO toInternalDTO(UserAddress address) {
        if (address == null) {
            return null;
        }
        AddressInternalDTO dto = new AddressInternalDTO();
        dto.setAddressId(address.getAddressId());
        dto.setUserId(address.getUserId());
        dto.setRecipientName(address.getRecipientName());
        dto.setRecipientPhone(address.getRecipientPhone());
        dto.setProvince(address.getProvince());
        dto.setCity(address.getCity());
        dto.setDistrict(address.getDistrict());
        dto.setStreetAddress(address.getStreetAddress());
        dto.setDetailAddress(address.getDetailAddress());
        dto.setFullAddress(address.getFullAddress());
        dto.setLongitude(address.getLongitude());
        dto.setLatitude(address.getLatitude());
        dto.setAddressLabel(address.getAddressLabel());
        dto.setIsDefault(address.getIsDefault());
        dto.setIsActive(address.getIsActive());
        return dto;
    }
}
