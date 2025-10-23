package com.njumarket.njumarket.controller.user;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.OrderDTO;
import com.njumarket.njumarket.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户订单控制器（买家和卖家功能）
 */
@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService orderService;

    /**
     * 创建订单（买家）
     */
    @PostMapping("/create")
    public Result createOrder(@RequestBody OrderDTO orderDTO) {
        return orderService.createOrder(orderDTO);
    }

    /**
     * 支付订单（买家）
     */
    @PostMapping("/{orderId}/pay")
    public Result payOrder(@PathVariable String orderId) {
        return orderService.payOrder(orderId);
    }

    /**
     * 确认收货（买家）
     */
    @PostMapping("/{orderId}/confirm")
    public Result confirmOrder(@PathVariable String orderId) {
        return orderService.confirmOrder(orderId);
    }

    /**
     * 取消订单（买家）
     */
    @PostMapping("/{orderId}/cancel")
    public Result cancelOrder(@PathVariable String orderId,
                            @RequestParam(required = false) String reason) {
        return orderService.cancelOrder(orderId, reason);
    }

    /**
     * 发货（卖家）
     */
    @PostMapping("/{orderId}/ship")
    public Result shipOrder(@PathVariable String orderId,
                          @RequestParam(required = false) String trackingNumber) {
        return orderService.shipOrder(orderId, trackingNumber);
    }

    /**
     * 获取我的买家订单
     */
    @GetMapping("/buyer")
    public Result getBuyerOrders(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               @RequestParam(required = false) String status) {
        return orderService.getBuyerOrders(page, size, status);
    }

    /**
     * 获取我的卖家订单
     */
    @GetMapping("/seller")
    public Result getSellerOrders(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                @RequestParam(required = false) String status) {
        return orderService.getSellerOrders(page, size, status);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{orderId}")
    public Result getOrderDetail(@PathVariable String orderId) {
        return orderService.getOrderDetail(orderId);
    }

    /**
     * 申请退款（买家）
     */
    @PostMapping("/{orderId}/refund")
    public Result requestRefund(@PathVariable String orderId,
                              @RequestParam String reason) {
        return orderService.requestRefund(orderId, reason);
    }

    /**
     * 处理退款申请（卖家）
     */
    @PostMapping("/{orderId}/refund/handle")
    public Result handleRefund(@PathVariable String orderId,
                             @RequestParam String decision,
                             @RequestParam(required = false) String remark) {
        return orderService.handleRefund(orderId, decision, remark);
    }

    /**
     * 修改订单可见性（同时设置卖家和买家可见性）
     */
    @PutMapping("/{orderId}/visibility")
    public Result updateOrderVisibility(@PathVariable String orderId,
                                      @RequestParam String visibility) {
        return orderService.updateOrderVisibility(orderId, visibility);
    }

    /**
     * 修改订单卖家可见性
     */
    @PutMapping("/{orderId}/seller-visibility")
    public Result updateOrderSellerVisibility(@PathVariable String orderId,
                                           @RequestParam String sellerVisibility) {
        return orderService.updateOrderSellerVisibility(orderId, sellerVisibility);
    }

    /**
     * 修改订单买家可见性
     */
    @PutMapping("/{orderId}/buyer-visibility")
    public Result updateOrderBuyerVisibility(@PathVariable String orderId,
                                          @RequestParam String buyerVisibility) {
        return orderService.updateOrderBuyerVisibility(orderId, buyerVisibility);
    }

    /**
     * 申请退货（买家功能）
     */
    @PostMapping("/{orderId}/return")
    public Result requestReturn(@PathVariable String orderId,
                              @RequestParam String returnReason) {
        return orderService.requestReturn(orderId, returnReason);
    }

    /**
     * 审批退货申请（卖家功能）
     */
    @PutMapping("/{orderId}/return/approve")
    public Result approveReturnRequest(@PathVariable String orderId,
                                    @RequestParam Boolean approved,
                                    @RequestParam(required = false) String rejectionReason) {
        return orderService.approveReturnRequest(orderId, approved, rejectionReason);
    }

    /**
     * 确认退货发货（买家功能）
     */
    @PutMapping("/{orderId}/return/shipment")
    public Result confirmReturnShipment(@PathVariable String orderId,
                                     @RequestParam String returnTrackingNumber) {
        return orderService.confirmReturnShipment(orderId, returnTrackingNumber);
    }

    /**
     * 完成退货（卖家功能）
     */
    @PutMapping("/{orderId}/return/complete")
    public Result completeReturn(@PathVariable String orderId) {
        return orderService.completeReturn(orderId);
    }

    /**
     * 获取退货申请列表（卖家功能）
     */
    @GetMapping("/returns")
    public Result getReturnRequests(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size,
                                 @RequestParam(required = false) String status) {
        return orderService.getReturnRequests(page, size, status);
    }

    /**
     * 获取我的退货记录（买家功能）
     */
    @GetMapping("/my-returns")
    public Result getMyReturnRecords(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) String status) {
        return orderService.getMyReturnRecords(page, size, status);
    }
}
