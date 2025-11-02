package com.njumarket.njumarket.dto;

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
    private String title;
    private String description;
    private Double price;
    private Integer stock;
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
