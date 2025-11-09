package com.njumarket.commodity.vo;

import com.njumarket.commodity.dto.CommodityDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品详情VO（包含额外信息）
 */
@Schema(description = "商品详情")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommodityDetailVO {
    @Schema(description = "商品信息")
    private CommodityDTO commodity;

    @Schema(description = "是否可以下单", example = "true")
    private Boolean canOrder;

    @Schema(description = "是否已下架", example = "false")
    private Boolean isOffShelf;

    @Schema(description = "状态消息", example = "商品可购买")
    private String statusMessage;
}

