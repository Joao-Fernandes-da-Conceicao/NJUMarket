package com.njumarket.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新订单地址DTO
 * 用于更新订单的收货地址快照（只更新订单快照，不影响商品）
 */
@Data
public class UpdateOrderAddressDTO {
    
    /**
     * 地址ID（可选，如果不传则使用默认地址）
     */
    private String addressId;
    
    /**
     * 地址快照字段（保存修改时的地址信息）
     */
    @NotBlank(message = "省份不能为空")
    private String province;
    
    @NotBlank(message = "城市不能为空")
    private String city;
    
    @NotBlank(message = "区/县不能为空")
    private String district;
    
    @NotBlank(message = "街道地址不能为空")
    private String streetAddress;
    
    private String detailAddress;
    
    @NotBlank(message = "完整地址不能为空")
    private String fullAddress;
    
    @NotBlank(message = "收货人姓名不能为空")
    private String recipientName;
    
    @NotBlank(message = "收货人电话不能为空")
    private String recipientPhone;
}

