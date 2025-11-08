package com.njumarket.order.client;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.entity.Commodity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Commodity Service Feign Client
 * 用于Order Service调用Commodity Service的内部接口
 */
@FeignClient(name = "njumarket-service-commodity", contextId = "commodityInternalClient", path = "/api/internal")
public interface CommodityClient {
    
    /**
     * 查询商品（带悲观锁，用于创建订单）
     */
    @GetMapping("/commodity/{commodityId}/for-update")
    Result getCommodityForUpdate(@PathVariable String commodityId);
    
    /**
     * 更新商品库存
     */
    @PostMapping("/commodity/{commodityId}/update-stock")
    Result updateCommodityStock(@PathVariable String commodityId, 
                                @RequestParam Integer quantity);
    
    /**
     * 恢复商品库存（用于订单取消、退款等场景）
     */
    @PostMapping("/commodity/{commodityId}/restore-stock")
    Result restoreCommodityStock(@PathVariable String commodityId, 
                                 @RequestParam Integer quantity);
}
