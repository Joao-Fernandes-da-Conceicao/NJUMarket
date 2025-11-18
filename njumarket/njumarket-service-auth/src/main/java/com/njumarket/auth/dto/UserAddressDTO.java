package com.njumarket.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户地址DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressDTO implements Serializable {
    
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
    
    // 时间戳
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

