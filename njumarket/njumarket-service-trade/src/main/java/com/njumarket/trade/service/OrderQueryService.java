package com.njumarket.trade.service;

import com.njumarket.njumarket.dto.Result;

import java.util.List;

/**
 * 订单查询服务
 * 负责订单详情、列表、批量状态等所有读操作
 */
public interface OrderQueryService {

    Result getOrderDetail(String orderId);

    Result getBuyerOrders(Integer page, Integer size, String status);

    Result getSellerOrders(Integer page, Integer size, String status);

    Result getOrdersBatchStatus(List<String> orderIds);

    Result queryOriginalCommodity(String orderId);

    Result getOrderHistory(String userId);

    Integer calcValidVolume(String userId);
}
