package com.njumarket.trade.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.trade.service.InventoryService;
import com.njumarket.trade.service.OrderAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 内部 API：订单与库存（原 order InternalController）
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderAdminService orderService;
    private final InventoryService inventoryService;

    // ========== 订单管理（管理端/跨服务调用）==========

    /**
     * 按内部 DTO 格式获取订单
     */
    @GetMapping("/order/{orderId}")
    public Result getOrderById(@PathVariable String orderId) {
        return orderService.getOrderByIdInternal(orderId);
    }

    /**
     * 批量查询订单基础信息（供 Message 服务增量轮询使用，无需用户 JWT）
     */
    @PostMapping("/orders/batch")
    public Result getOrdersBatch(@RequestBody List<String> orderIds) {
        return orderService.getOrdersByIdsInternal(orderIds);
    }

    /**
     * 管理端全字段覆盖更新订单
     */
    @PutMapping("/order/{orderId}/full")
    public Result updateOrderFull(@PathVariable String orderId,
                                  @RequestBody Map<String, Object> payload) {
        return orderService.updateOrderFull(orderId, payload);
    }

    /**
     * 管理端删除订单
     */
    @DeleteMapping("/order/{orderId}")
    public Result deleteOrder(@PathVariable String orderId) {
        return orderService.deleteOrderInternal(orderId);
    }

    /**
     * 检查商品是否存在有效订单（供商品服务在删除商品前调用）
     */
    @GetMapping("/order/check-commodity/{commodityId}")
    public Result checkCommodityHasOrders(@PathVariable String commodityId) {
        return orderService.checkCommodityHasOrders(commodityId);
    }

    /**
     * 管理端分页查询订单列表
     */
    @GetMapping("/orders")
    public Result listOrders(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String sellerVisibility,
                             @RequestParam(required = false) String buyerVisibility,
                             @RequestParam(required = false) String sortProp,
                             @RequestParam(required = false) String sortOrder) {
        return orderService.listOrdersInternal(page, size, keyword, status,
                sellerVisibility, buyerVisibility, sortProp, sortOrder);
    }

    // ========== 库存管理（供商品服务调用）==========

    /**
     * 同步商品库存（商品上架时调用）
     * 请求体：{ "commodityId": "...", "availableQuantity": N, "totalQuantity": N }
     */
    @PostMapping("/inventory/sync")
    public Result syncInventory(@RequestBody Map<String, Object> payload) {
        String commodityId = (String) payload.get("commodityId");
        int available = payload.get("availableQuantity") instanceof Number
                ? ((Number) payload.get("availableQuantity")).intValue() : 0;
        int total = payload.get("totalQuantity") instanceof Number
                ? ((Number) payload.get("totalQuantity")).intValue() : available;
        return inventoryService.syncInventory(commodityId, available, total);
    }

    /**
     * 按新总量调整库存（在架商品修改库存后调用）
     * 请求体：{ "commodityId": "...", "newTotalQuantity": N }
     */
    @PostMapping("/inventory/adjust")
    public Result adjustInventory(@RequestBody Map<String, Object> payload) {
        String commodityId = (String) payload.get("commodityId");
        int newTotal = payload.get("newTotalQuantity") instanceof Number
                ? ((Number) payload.get("newTotalQuantity")).intValue() : 0;
        return inventoryService.adjustInventory(commodityId, newTotal);
    }

    /**
     * 归零商品库存（商品下架/设草稿时调用）
     */
    @PostMapping("/inventory/zero/{commodityId}")
    public Result zeroInventory(@PathVariable String commodityId) {
        return inventoryService.zeroInventory(commodityId);
    }

    /**
     * 查询商品可用库存（供内部诊断）
     */
    @GetMapping("/inventory/{commodityId}")
    public Result getInventory(@PathVariable String commodityId) {
        return inventoryService.getInventory(commodityId);
    }
}
