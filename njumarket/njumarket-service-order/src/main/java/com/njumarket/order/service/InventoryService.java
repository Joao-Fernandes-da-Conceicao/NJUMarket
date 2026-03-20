package com.njumarket.order.service;

import com.njumarket.njumarket.dto.Result;

/**
 * 库存服务接口
 * 封装 commodity_inventory 表的增删改查，供内部控制器及订单流程调用
 */
public interface InventoryService {

    /**
     * 同步商品库存（商品上架时由商品服务调用）
     * 使用 upsert：记录不存在则创建，已存在则覆盖
     */
    Result syncInventory(String commodityId, int availableQuantity, int totalQuantity);

    /**
     * 按新总量调整库存（商品在架时卖家修改库存后调用）
     * 语义：total = newTotal；available += (newTotal - oldTotal)，夹在 [0, newTotal]
     */
    Result adjustInventory(String commodityId, int newTotalQuantity);

    /**
     * 归零商品库存（商品下架/设草稿时调用，禁止后续下单）
     */
    Result zeroInventory(String commodityId);

    /**
     * 查询商品可用库存
     */
    Result getInventory(String commodityId);
}
