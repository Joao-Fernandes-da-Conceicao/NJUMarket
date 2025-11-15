package com.njumarket.njumarket.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品事件消息
 * 用于跨服务异步通知商品变更
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommodityEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID（接收通知的用户）
     */
    private String userId;
    
    /**
     * 商品ID
     */
    private String commodityId;
    
    /**
     * 操作类型：COMMODITY_CREATED, COMMODITY_UPDATED, COMMODITY_SHELVED, COMMODITY_UNSHELVED 等
     */
    private String operation;
    
    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;
}

