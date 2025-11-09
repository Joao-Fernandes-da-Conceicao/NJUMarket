package com.njumarket.notification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 增量更新结果VO（推送服务）
 * 使用 Map 避免循环依赖
 */
@Schema(description = "增量更新结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncrementalUpdateResultVO {
    @Schema(description = "变更的商品列表（Map格式）")
    private List<Map<String, Object>> commodities;
    
    @Schema(description = "变更的订单列表（Map格式）")
    private List<Map<String, Object>> orders;
}

