package com.njumarket.trade.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.trade.dto.UpdateOrderAddressDTO;

/**
 * 订单属性管理服务
 * 负责可见性调整和地址变更等非状态流转的写操作
 */
public interface OrderManageService {

    Result updateOrderVisibility(String orderId, String visibility);

    Result updateOrderSellerVisibility(String orderId, String sellerVisibility);

    Result updateOrderBuyerVisibility(String orderId, String buyerVisibility);

    Result updateOrderShippingAddress(String orderId, UpdateOrderAddressDTO addressDTO);
}
