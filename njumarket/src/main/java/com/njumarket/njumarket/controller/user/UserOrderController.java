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
     * 评价订单
     */
    @PostMapping("/{orderId}/rate")
    public Result rateOrder(@PathVariable String orderId,
                          @RequestParam Integer rating,
                          @RequestParam(required = false) String comment) {
        return orderService.rateOrder(orderId, rating, comment);
    }
}
