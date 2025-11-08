package com.njumarket.njumarket.vo;

import com.njumarket.njumarket.dto.CommodityDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品分页结果VO
 */
@Schema(description = "商品分页结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommodityPageResultVO {
    @Schema(description = "商品列表")
    private List<CommodityDTO> commodities;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "总页数", example = "10")
    private Integer pages;

    @Schema(description = "当前页码", example = "1")
    private Integer current;

    @Schema(description = "每页大小", example = "10")
    private Integer size;
}

