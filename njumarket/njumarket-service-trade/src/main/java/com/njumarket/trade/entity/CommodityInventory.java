package com.njumarket.trade.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品库存实体，归属于订单服务。
 *
 * 设计原则：
 *   - 库存从商品服务解耦：商品服务 commodities.stock 只作为卖家设定的"初始/展示库存"。
 *   - 超卖防护在订单服务本地完成（条件更新 WHERE available_quantity >= ?），
 *     不再跨服务调用 commodity-service 扣减库存。
 *   - 商品上架时，商品服务调用订单服务内部接口同步库存初始值；
 *     下架/草稿时将 available_quantity 归零，禁止下单。
 */
@Entity
@Table(name = "commodity_inventory", schema = "nju_market")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommodityInventory {

    /**
     * 商品 ID，与 commodities.commodity_id 一一对应，不设外键约束（跨服务）。
     */
    @Id
    @Column(name = "commodity_id", length = 50)
    private String commodityId;

    /**
     * 当前可用库存（用于下单判断，不可为负）。
     * 原子扣减：UPDATE ... SET available_quantity = available_quantity - ? WHERE available_quantity >= ?
     */
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity = 0;

    /**
     * 卖家设定的总库存（来自 commodities.stock，仅作参考）。
     */
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
