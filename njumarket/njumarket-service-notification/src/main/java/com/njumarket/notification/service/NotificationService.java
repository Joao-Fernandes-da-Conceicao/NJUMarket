package com.njumarket.notification.service;

/**
 * 统一推送服务接口
 * 提供所有类型的推送功能：订单变更、商品变更、聊天消息等
 */
public interface NotificationService {
    
    /**
     * 推送订单变更通知。
     *
     * @param userId        通知接收方用户 ID
     * @param orderId       订单 ID
     * @param operation     操作类型（ORDER_CREATED / ORDER_PAID / ORDER_SHIPPED 等）
     * @param recipientRole 接收方在该订单中的角色（{@code "BUYER"} 或 {@code "SELLER"}），
     *                      用于前端决定亮哪个角标；传 {@code null} 时降级为操作类型推断（向后兼容）
     */
    void pushOrderChange(String userId, String orderId, String operation, String recipientRole);

    /**
     * @deprecated 兼容旧调用方；新代码请使用 {@link #pushOrderChange(String, String, String, String)}
     */
    @Deprecated
    default void pushOrderChange(String userId, String orderId, String operation) {
        pushOrderChange(userId, orderId, operation, null);
    }
    
    /**
     * 推送聊天消息通知
     * @param userId 用户ID
     * @param messageData 消息数据
     */
    void pushMessage(String userId, Object messageData);
    
    /**
     * 推送未读消息数更新
     * @param userId 用户ID
     * @param unreadCount 未读消息数
     */
    void pushUnreadCountUpdate(String userId, Integer unreadCount);
    
    /**
     * 通用推送方法（支持所有消息类型）
     * @param userId 用户ID
     * @param messageData 消息数据
     * @param messageType 消息类型（CONVERSATION_RESTORED、MESSAGE_READ等）
     */
    void pushGenericMessage(String userId, Object messageData, String messageType);
}

