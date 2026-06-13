package com.njumarket.trade.vo;

import com.njumarket.trade.dto.OrderDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单分页结果VO
 */
@Schema(description = "订单分页结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPageResultVO {
    @Schema(description = "订单列表")
    private List<OrderDTO> orders;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "总页数", example = "10")
    private Integer pages;

    @Schema(description = "当前页码", example = "1")
    private Integer current;

    @Schema(description = "每页大小", example = "10")
    private Integer size;
}

