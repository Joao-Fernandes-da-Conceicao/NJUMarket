package com.njumarket.njumarket.controller.user;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.OrderDTO;
import com.njumarket.njumarket.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户订单管理", description = "买家订单和卖家订单的完整管理功能")
@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单", description = "买家创建新订单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "商品不存在")
    })
    @PostMapping("/create")
    public Result createOrder(@RequestBody OrderDTO orderDTO) {
        return orderService.createOrder(orderDTO);
    }

    @Operation(summary = "支付订单", description = "买家支付订单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "支付成功"),
        @ApiResponse(responseCode = "400", description = "订单状态不允许支付"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PostMapping("/{orderId}/pay")
    public Result payOrder(@Parameter(description = "订单ID", required = true) @PathVariable String orderId) {
        return orderService.payOrder(orderId);
    }

    @Operation(summary = "确认收货", description = "买家确认收货")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "确认成功"),
        @ApiResponse(responseCode = "400", description = "订单状态不允许确认收货"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PostMapping("/{orderId}/confirm")
    public Result confirmOrder(@Parameter(description = "订单ID", required = true) @PathVariable String orderId) {
        return orderService.confirmOrder(orderId);
    }

    @Operation(summary = "取消订单", description = "买家取消订单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "取消成功"),
        @ApiResponse(responseCode = "400", description = "订单状态不允许取消"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PostMapping("/{orderId}/cancel")
    public Result cancelOrder(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        return orderService.cancelOrder(orderId, reason);
    }

    @Operation(summary = "发货", description = "卖家发货")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "发货成功"),
        @ApiResponse(responseCode = "400", description = "订单状态不允许发货"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PostMapping("/{orderId}/ship")
    public Result shipOrder(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                          @Parameter(description = "快递单号") @RequestParam(required = false) String trackingNumber) {
        return orderService.shipOrder(orderId, trackingNumber);
    }

    @Operation(summary = "查询原商品信息", description = "基于商品快照查询原商品信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "订单或商品不存在")
    })
    @GetMapping("/{orderId}/query-commodity")
    public Result queryOriginalCommodity(@Parameter(description = "订单ID", required = true) @PathVariable String orderId) {
        return orderService.queryOriginalCommodity(orderId);
    }
    
    @Operation(summary = "基于快照创建新订单", description = "使用订单快照创建新订单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PostMapping("/{orderId}/create-from-snapshot")
    public Result createOrderFromSnapshot(@Parameter(description = "订单ID", required = true) @PathVariable String orderId, 
                                         @RequestBody Map<String, Object> orderData) {
        return orderService.createOrderFromSnapshot(orderId, orderData);
    }

    @Operation(summary = "获取买家订单列表", description = "获取当前用户的买家订单列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/buyer")
    public Result getBuyerOrders(@Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
                               @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
                               @Parameter(description = "订单状态") @RequestParam(required = false) String status) {
        return orderService.getBuyerOrders(page, size, status);
    }

    @Operation(summary = "获取卖家订单列表", description = "获取当前用户的卖家订单列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/seller")
    public Result getSellerOrders(@Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
                                @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
                                @Parameter(description = "订单状态") @RequestParam(required = false) String status) {
        return orderService.getSellerOrders(page, size, status);
    }

    @Operation(summary = "获取订单详情", description = "获取指定订单的详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @GetMapping("/{orderId}")
    public Result getOrderDetail(@Parameter(description = "订单ID", required = true) @PathVariable String orderId) {
        return orderService.getOrderDetail(orderId);
    }

    @Operation(summary = "申请退款", description = "买家申请退款")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "申请成功"),
        @ApiResponse(responseCode = "400", description = "订单状态不允许申请退款"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PostMapping("/{orderId}/refund")
    public Result requestRefund(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                              @Parameter(description = "退款原因", required = true) @RequestParam String reason) {
        return orderService.requestRefund(orderId, reason);
    }

    @Operation(summary = "处理退款申请", description = "卖家处理退款申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "处理成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PostMapping("/{orderId}/refund/handle")
    public Result handleRefund(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                             @Parameter(description = "处理决定", required = true, example = "APPROVE") @RequestParam String decision,
                             @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        return orderService.handleRefund(orderId, decision, remark);
    }

    @Operation(summary = "修改订单可见性", description = "同时设置卖家和买家可见性")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PutMapping("/{orderId}/visibility")
    public Result updateOrderVisibility(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                                      @Parameter(description = "可见性", required = true, example = "PUBLIC") @RequestParam String visibility) {
        return orderService.updateOrderVisibility(orderId, visibility);
    }

    @Operation(summary = "修改订单卖家可见性", description = "修改订单对卖家的可见性")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PutMapping("/{orderId}/seller-visibility")
    public Result updateOrderSellerVisibility(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                                           @Parameter(description = "卖家可见性", required = true, example = "PUBLIC") @RequestParam String sellerVisibility) {
        return orderService.updateOrderSellerVisibility(orderId, sellerVisibility);
    }

    @Operation(summary = "修改订单买家可见性", description = "修改订单对买家的可见性")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PutMapping("/{orderId}/buyer-visibility")
    public Result updateOrderBuyerVisibility(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                                          @Parameter(description = "买家可见性", required = true, example = "PUBLIC") @RequestParam String buyerVisibility) {
        return orderService.updateOrderBuyerVisibility(orderId, buyerVisibility);
    }

    @Operation(summary = "申请退货", description = "买家申请退货")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "申请成功"),
        @ApiResponse(responseCode = "400", description = "订单状态不允许申请退货"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PostMapping("/{orderId}/return")
    public Result requestReturn(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                              @Parameter(description = "退货原因", required = true) @RequestParam String returnReason) {
        return orderService.requestReturn(orderId, returnReason);
    }

    @Operation(summary = "审批退货申请", description = "卖家审批退货申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审批成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PutMapping("/{orderId}/return/approve")
    public Result approveReturnRequest(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                                    @Parameter(description = "是否批准", required = true) @RequestParam Boolean approved,
                                    @Parameter(description = "拒绝原因") @RequestParam(required = false) String rejectionReason) {
        return orderService.approveReturnRequest(orderId, approved, rejectionReason);
    }

    @Operation(summary = "确认退货发货", description = "买家确认退货发货")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "确认成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PutMapping("/{orderId}/return/shipment")
    public Result confirmReturnShipment(@Parameter(description = "订单ID", required = true) @PathVariable String orderId,
                                     @Parameter(description = "退货快递单号", required = true) @RequestParam String returnTrackingNumber) {
        return orderService.confirmReturnShipment(orderId, returnTrackingNumber);
    }

    @Operation(summary = "完成退货", description = "卖家完成退货处理")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "完成成功"),
        @ApiResponse(responseCode = "400", description = "订单状态不允许完成退货"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PutMapping("/{orderId}/return/complete")
    public Result completeReturn(@Parameter(description = "订单ID", required = true) @PathVariable String orderId) {
        return orderService.completeReturn(orderId);
    }

    @Operation(summary = "获取退货申请列表", description = "卖家获取退货申请列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/returns")
    public Result getReturnRequests(@Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
                                 @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
                                 @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return orderService.getReturnRequests(page, size, status);
    }

    @Operation(summary = "获取我的退货记录", description = "买家获取退货记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/my-returns")
    public Result getMyReturnRecords(@Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
                                   @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
                                   @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return orderService.getMyReturnRecords(page, size, status);
    }
    
    // ✅ 批量查询订单状态（用于聊天界面，轻量级查询）
    @Operation(summary = "批量查询订单状态", description = "批量查询订单基本信息，用于聊天界面显示，只返回轻量级信息")
    @PostMapping("/batch-status")
    public Result getOrdersBatchStatus(@RequestBody List<String> orderIds) {
        return orderService.getOrdersBatchStatus(orderIds);
    }
}
