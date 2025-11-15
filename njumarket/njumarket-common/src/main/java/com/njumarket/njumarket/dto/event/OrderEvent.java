package com.njumarket.njumarket.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单事件消息
 * 用于跨服务异步通知订单变更
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID（接收通知的用户）
     */
    private String userId;
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 操作类型：ORDER_CREATED, ORDER_PAID, ORDER_SHIPPED, ORDER_COMPLETED, ORDER_CANCELLED 等
     */
    private String operation;
    
    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;
}

