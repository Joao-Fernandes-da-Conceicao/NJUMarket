package com.njumarket.njumarket.vo;

import com.njumarket.njumarket.dto.CommodityDTO;
import com.njumarket.njumarket.dto.OrderDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 增量更新结果VO
 */
@Schema(description = "增量更新结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncrementalUpdateResultVO {
    @Schema(description = "变更的商品列表")
    private List<CommodityDTO> commodities;

    @Schema(description = "变更的订单列表")
    private List<OrderDTO> orders;
}

