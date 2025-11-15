package com.njumarket.notification.service;

/**
 * 统一推送服务接口
 * 提供所有类型的推送功能：订单变更、商品变更、聊天消息等
 */
public interface NotificationService {
    
    /**
     * 推送订单变更通知
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param operation 操作类型（CREATE, UPDATE, PAY, SHIP等）
     */
    void pushOrderChange(String userId, String orderId, String operation);
    
    /**
     * 推送商品变更通知
     * @param userId 用户ID
     * @param commodityId 商品ID
     * @param operation 操作类型（CREATE, UPDATE, SHELF, UNSHELF等）
     */
    void pushCommodityChange(String userId, String commodityId, String operation);
    
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

