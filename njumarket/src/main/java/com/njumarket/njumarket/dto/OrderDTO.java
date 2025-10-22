package com.njumarket.njumarket.dto;

import lombok.Data;

/**
 * 订单数据传输对象
 */
@Data
public class OrderDTO {
    private String orderId;
    private String buyerId;
    private String sellerId;
    private String commodityId;
    private String orderStatus;
    private Double payAmount;
    private Integer quantity;
    private String deliveryAddress;
    private String remark;
}
