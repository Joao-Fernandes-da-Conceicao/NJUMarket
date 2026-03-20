package com.njumarket.order.service;

import com.njumarket.njumarket.dto.Result;

/**
 * 退款退货服务
 * 负责退款申请、退货申请及其全生命周期管理
 */
public interface OrderReturnService {

    // ===== 退款（资金流转） =====

    Result requestRefund(String orderId, String reason);

    Result handleRefund(String orderId, String decision, String remark);

    // ===== 退货（实物流转） =====

    Result requestReturn(String orderId, String returnReason);

    Result approveReturnRequest(String orderId, Boolean approved, String rejectionReason);

    Result confirmReturnShipment(String orderId, String returnTrackingNumber);

    Result completeReturn(String orderId);

    // ===== 列表查询 =====

    Result getReturnRequests(Integer page, Integer size, String status);

    Result getMyReturnRecords(Integer page, Integer size, String status);
}
