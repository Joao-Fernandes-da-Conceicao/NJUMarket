package com.njumarket.order.client;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.client.fallback.CommodityClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Commodity Service 内部 Feign Client（精简版）
 *
 * 历史说明：
 *   原本此 Client 承担了跨服务库存扣减（updateCommodityStock / restoreCommodityStock）。
 *   库存已迁移至订单服务本地管理（commodity_inventory 表），上述方法已废弃并移除。
 *   商品查询统一使用 CommodityQueryClient（公开/用户接口），此 Client 仅供未来内部接口扩展保留。
 */
@FeignClient(name = "njumarket-service-commodity",
             contextId = "commodityInternalClient",
             path = "/api/internal",
             fallback = CommodityClientFallback.class)
public interface CommodityClient {

    /**
     * 根据 ID 查询商品内部详情（管理端接口，不含锁）。
     */
    @GetMapping("/commodity/{commodityId}")
    Result getCommodityById(@PathVariable String commodityId);
}
