package com.njumarket.trade.repository;

import com.njumarket.trade.entity.CommodityInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CommodityInventoryRepository extends JpaRepository<CommodityInventory, String> {

    Optional<CommodityInventory> findByCommodityId(String commodityId);

    /**
     * 原子库存扣减。
     * 仅当 available_quantity >= quantity 时更新，返回受影响行数。
     * 返回 0 表示库存不足，无需额外加锁。
     */
    @Modifying
    @Query("UPDATE CommodityInventory i SET i.availableQuantity = i.availableQuantity - :qty, " +
           "i.updatedAt = :now WHERE i.commodityId = :cid AND i.availableQuantity >= :qty")
    int deductStock(@Param("cid") String commodityId,
                    @Param("qty") Integer qty,
                    @Param("now") LocalDateTime now);

    /**
     * 原子库存回补（取消/退货时调用）。
     * 不设上限检查（调用方保证不会超过 total_quantity）。
     */
    @Modifying
    @Query("UPDATE CommodityInventory i SET i.availableQuantity = i.availableQuantity + :qty, " +
           "i.updatedAt = :now WHERE i.commodityId = :cid")
    int restoreStock(@Param("cid") String commodityId,
                     @Param("qty") Integer qty,
                     @Param("now") LocalDateTime now);

    /**
     * 库存同步（上架时由商品服务调用）：upsert。
     * 使用原生 SQL 的 ON CONFLICT DO UPDATE 以支持 upsert。
     */
    @Modifying
    @Query(value = "INSERT INTO nju_market.commodity_inventory " +
                   "(commodity_id, available_quantity, total_quantity, updated_at) " +
                   "VALUES (:cid, :qty, :total, :now) " +
                   "ON CONFLICT (commodity_id) DO UPDATE " +
                   "SET available_quantity = :qty, total_quantity = :total, updated_at = :now",
           nativeQuery = true)
    void upsertStock(@Param("cid") String commodityId,
                     @Param("qty") Integer availableQuantity,
                     @Param("total") Integer totalQuantity,
                     @Param("now") LocalDateTime now);

    /**
     * 归零库存（下架/草稿时调用，禁止下单）。
     */
    @Modifying
    @Query("UPDATE CommodityInventory i SET i.availableQuantity = 0, i.updatedAt = :now " +
           "WHERE i.commodityId = :cid")
    int zeroStock(@Param("cid") String commodityId, @Param("now") LocalDateTime now);
}
