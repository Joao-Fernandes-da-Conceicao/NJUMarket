package com.njumarket.trade.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 基于订单快照创建新订单的请求DTO
 * 用于替换 Map<String, Object> orderData
 */
@Data
public class OrderSnapshotDTO {

    /**
     * 购买数量（必填）
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    /**
     * 收货地址（可选，如果为空则使用原订单的地址）
     */
    private String shippingAddress;

    /**
     * 备注（可选）
     */
    private String remark;
}

