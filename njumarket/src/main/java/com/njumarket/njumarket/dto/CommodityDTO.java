package com.njumarket.njumarket.dto;

import lombok.Data;

import java.util.List;

/**
 * 商品数据传输对象
 */
@Data
public class CommodityDTO {
    private String commodityId;
    private String sellerId;
    private String title;
    private String description;
    private Double price;
    private Integer stock;
    private String location;
    private String commodityStatus;
    private List<String> images;
    private String category;
}
