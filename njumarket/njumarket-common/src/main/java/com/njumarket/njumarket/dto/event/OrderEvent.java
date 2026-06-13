package com.njumarket.njumarket.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单事件消息，用于跨服务异步通知订单变更。
 *
 * <p>{@code userId} 是已确定的通知接收方，{@code recipientRole} 明确标注其身份（BUYER/SELLER），
 * 供 Notification 服务决定在前端亮哪个角标，无需根据操作类型反向推断。
 * 若未设置 {@code recipientRole}，Notification 服务将降级为根据 {@code operation} 推断（向后兼容）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（接收通知的用户，即操作方的对方）
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
     * 接收方在该订单中的角色：{@code "BUYER"} 或 {@code "SELLER"}。
     * 由订单服务在发布事件时明确设置，避免 Notification 服务对 ORDER_CANCELLED 等
     * 双向操作的角色判断出错。
     */
    private String recipientRole;

    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;
}

