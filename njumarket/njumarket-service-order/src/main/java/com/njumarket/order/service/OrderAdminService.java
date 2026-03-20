package com.njumarket.order.service;

import com.njumarket.njumarket.dto.Result;

import java.util.Map;

/**
 * 订单管理端/内部服务
 * 供 InternalController 使用，不对前端用户暴露
 */
public interface OrderAdminService {

    Result getOrderByIdInternal(String orderId);

    Result updateOrderFull(String orderId, Map<String, Object> payload);

    Result deleteOrderInternal(String orderId);

    Result checkCommodityHasOrders(String commodityId);

    Result listOrdersInternal(Integer page, Integer size, String keyword, String status,
                              String sellerVisibility, String buyerVisibility,
                              String sortProp, String sortOrder);
}
