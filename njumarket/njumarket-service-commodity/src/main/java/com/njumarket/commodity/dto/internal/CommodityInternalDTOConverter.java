package com.njumarket.commodity.dto.internal;

import com.njumarket.commodity.entity.Commodity;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品内部 DTO 转换器（Commodity Service专用）
 * 用于将 Commodity 实体转换为内部 DTO（用于服务间通信）
 */
@Component
public class CommodityInternalDTOConverter {
    
    /**
     * 将 Commodity 转换为 CommodityInternalDTO
     */
    public CommodityInternalDTO toInternalDTO(Commodity commodity) {
        if (commodity == null) {
            return null;
        }
        
        CommodityInternalDTO dto = new CommodityInternalDTO();
        dto.setCommodityId(commodity.getCommodityId());
        dto.setSellerId(commodity.getSellerId());
        dto.setTitle(commodity.getTitle());
        dto.setDescription(commodity.getDescription());
        // 转换Double为BigDecimal
        dto.setPrice(commodity.getPrice() != null ? java.math.BigDecimal.valueOf(commodity.getPrice()) : null);
        dto.setStock(commodity.getStock());
        dto.setCategory(commodity.getCategory());
        dto.setConditionLevel(commodity.getConditionLevel());
        dto.setStatus(commodity.getCommodityStatus()); // 使用commodityStatus
        dto.setSellerVisibility(commodity.getSellerVisibility());
        dto.setBuyerVisibility(commodity.getBuyerVisibility());
        dto.setLocation(commodity.getLocation());
        dto.setImages(commodity.getImages()); // 添加图片字段转换
        dto.setCreateTime(commodity.getPublishTime()); // 使用publishTime作为createTime
        dto.setUpdateTime(null); // Commodity实体没有updateTime字段
        return dto;
    }
    
    /**
     * 批量转换 Commodity 列表
     */
    public List<CommodityInternalDTO> toCommodityInternalDTOList(List<Commodity> commodities) {
        if (commodities == null) {
            return null;
        }
        return commodities.stream()
            .map(this::toInternalDTO)
            .collect(Collectors.toList());
    }
}

