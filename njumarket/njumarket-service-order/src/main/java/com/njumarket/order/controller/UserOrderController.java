package com.njumarket.order.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.dto.OrderDTO;
import com.njumarket.order.dto.OrderSnapshotDTO;
import com.njumarket.order.dto.UpdateOrderAddressDTO;
import com.njumarket.order.service.OrderLifecycleService;
import com.njumarket.order.service.OrderManageService;
import com.njumarket.order.service.OrderQueryService;
import com.njumarket.order.service.OrderReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户订单管理", description = "买家订单和卖家订单的完整管理功能")
@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderLifecycleService lifecycleService;
    private final OrderQueryService queryService;
    private final OrderReturnService returnService;
    private final OrderManageService manageService;

    // ===== 生命周期 =====

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        return lifecycleService.createOrder(orderDTO);
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{orderId}/pay")
    public Result payOrder(@Parameter(description = "订单ID", required = true) @PathVariable String orderId) {
        return lifecycleService.payOrder(orderId);
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{orderId}/confirm")
    public Result confirmOrder(@PathVariable String orderId) {
        return lifecycleService.confirmOrder(orderId);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{orderId}/cancel")
    public Result cancelOrder(@PathVariable String orderId,
                              @RequestParam(required = false) String reason) {
        return lifecycleService.cancelOrder(orderId, reason);
    }

    @Operation(summary = "发货")
    @PostMapping("/{orderId}/ship")
    public Result shipOrder(@PathVariable String orderId,
                            @RequestParam(required = false) String trackingNumber) {
        return lifecycleService.shipOrder(orderId, trackingNumber);
    }

    @Operation(summary = "基于快照创建新订单")
    @PostMapping("/{orderId}/create-from-snapshot")
    public Result createOrderFromSnapshot(@PathVariable String orderId,
                                          @Valid @RequestBody OrderSnapshotDTO orderSnapshotDTO) {
        return lifecycleService.createOrderFromSnapshot(orderId, orderSnapshotDTO);
    }

    // ===== 查询 =====

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderId}")
    public Result getOrderDetail(@PathVariable String orderId) {
        return queryService.getOrderDetail(orderId);
    }

    @Operation(summary = "获取买家订单列表")
    @GetMapping("/buyer")
    public Result getBuyerOrders(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size,
                                 @RequestParam(required = false) String status) {
        return queryService.getBuyerOrders(page, size, status);
    }

    @Operation(summary = "获取卖家订单列表")
    @GetMapping("/seller")
    public Result getSellerOrders(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String status) {
        return queryService.getSellerOrders(page, size, status);
    }

    @Operation(summary = "批量查询订单状态（聊天界面）")
    @PostMapping("/batch-status")
    public Result getOrdersBatchStatus(@RequestBody List<String> orderIds) {
        return queryService.getOrdersBatchStatus(orderIds);
    }

    @Operation(summary = "查询原商品信息（基于快照）")
    @GetMapping("/{orderId}/query-commodity")
    public Result queryOriginalCommodity(@PathVariable String orderId) {
        return queryService.queryOriginalCommodity(orderId);
    }

    // ===== 退款退货 =====

    @Operation(summary = "申请退款")
    @PostMapping("/{orderId}/refund")
    public Result requestRefund(@PathVariable String orderId,
                                @RequestParam String reason) {
        return returnService.requestRefund(orderId, reason);
    }

    @Operation(summary = "处理退款申请（卖家）")
    @PostMapping("/{orderId}/refund/handle")
    public Result handleRefund(@PathVariable String orderId,
                               @RequestParam String decision,
                               @RequestParam(required = false) String remark) {
        return returnService.handleRefund(orderId, decision, remark);
    }

    @Operation(summary = "申请退货（买家）")
    @PostMapping("/{orderId}/return")
    public Result requestReturn(@PathVariable String orderId,
                                @RequestParam String returnReason) {
        return returnService.requestReturn(orderId, returnReason);
    }

    @Operation(summary = "审批退货申请（卖家）")
    @PutMapping("/{orderId}/return/approve")
    public Result approveReturnRequest(@PathVariable String orderId,
                                       @RequestParam Boolean approved,
                                       @RequestParam(required = false) String rejectionReason) {
        return returnService.approveReturnRequest(orderId, approved, rejectionReason);
    }

    @Operation(summary = "确认退货发货（买家）")
    @PutMapping("/{orderId}/return/shipment")
    public Result confirmReturnShipment(@PathVariable String orderId,
                                        @RequestParam String returnTrackingNumber) {
        return returnService.confirmReturnShipment(orderId, returnTrackingNumber);
    }

    @Operation(summary = "完成退货（卖家）")
    @PutMapping("/{orderId}/return/complete")
    public Result completeReturn(@PathVariable String orderId) {
        return returnService.completeReturn(orderId);
    }

    @Operation(summary = "获取退货申请列表（卖家）")
    @GetMapping("/returns")
    public Result getReturnRequests(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) String status) {
        return returnService.getReturnRequests(page, size, status);
    }

    @Operation(summary = "获取我的退货记录（买家）")
    @GetMapping("/my-returns")
    public Result getMyReturnRecords(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     @RequestParam(required = false) String status) {
        return returnService.getMyReturnRecords(page, size, status);
    }

    // ===== 属性管理 =====

    @Operation(summary = "修改订单可见性（卖家和买家同时）")
    @PutMapping("/{orderId}/visibility")
    public Result updateOrderVisibility(@PathVariable String orderId, @RequestParam String visibility) {
        return manageService.updateOrderVisibility(orderId, visibility);
    }

    @Operation(summary = "修改订单卖家可见性")
    @PutMapping("/{orderId}/seller-visibility")
    public Result updateOrderSellerVisibility(@PathVariable String orderId, @RequestParam String sellerVisibility) {
        return manageService.updateOrderSellerVisibility(orderId, sellerVisibility);
    }

    @Operation(summary = "修改订单买家可见性")
    @PutMapping("/{orderId}/buyer-visibility")
    public Result updateOrderBuyerVisibility(@PathVariable String orderId, @RequestParam String buyerVisibility) {
        return manageService.updateOrderBuyerVisibility(orderId, buyerVisibility);
    }

    @Operation(summary = "更新订单收货地址")
    @PutMapping("/{orderId}/shipping-address")
    public Result updateOrderShippingAddress(@PathVariable String orderId,
                                             @Valid @RequestBody UpdateOrderAddressDTO addressDTO) {
        return manageService.updateOrderShippingAddress(orderId, addressDTO);
    }
}
