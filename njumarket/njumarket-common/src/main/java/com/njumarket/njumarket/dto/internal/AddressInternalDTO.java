package com.njumarket.njumarket.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 地址内部传输对象（用于服务间通信）
 * 不包含关联对象，只包含必要字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressInternalDTO implements Serializable {
    private String addressId;
    private String userId;
    
    // 地址基本信息
    private String recipientName;
    private String recipientPhone;
    
    // 地址层级（省市区）
    private String province;
    private String city;
    private String district;
    
    // 详细地址
    private String streetAddress;
    private String detailAddress;
    private String fullAddress;
    
    // 地理位置
    private Double longitude;
    private Double latitude;
    
    // 地址标签和状态
    private String addressLabel; // HOME, SCHOOL, COMPANY, OTHER
    private Boolean isDefault;
    private Boolean isActive;
    
    // 不包含 User 关联对象
}

