package com.njumarket.njumarket.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品数据传输对象
 */
@Data
public class CommodityDTO {
    private String commodityId;
    private String sellerId;
    private String sellerNickname; // 卖家昵称（批量查询优化）
    private String sellerAvatar;   // 卖家头像（批量查询优化）

    @NotBlank(message = "商品标题不能为空")
    @Size(max = 200, message = "商品标题长度不能超过200个字符")
    private String title;

    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private Double price;

    @NotNull(message = "商品库存不能为空")
    @Min(value = 0, message = "商品库存不能为负数")
    private Integer stock;

    @NotBlank(message = "商品位置不能为空")
    private String location;
    private LocalDateTime publishTime;
    private String commodityStatus;
    private String sellerVisibility;
    private String buyerVisibility;
    private String category;
    private String conditionLevel;
    private List<String> images;
    private Integer clickCount;
}

