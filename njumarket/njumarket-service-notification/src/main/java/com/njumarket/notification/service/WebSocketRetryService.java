package com.njumarket.notification.service;

/**
 * WebSocket消息推送重试服务接口
 */
public interface WebSocketRetryService {
    
    /**
     * 尝试推送消息，如果失败则加入重试队列
     * 
     * @param receiverId 接收方用户ID
     * @param messageData 消息数据
     * @param messageType 消息类型（ORDER_CHANGE, COMMODITY_CHANGE, MESSAGE_NEW, UNREAD_COUNT_UPDATE等）
     * @param messageId 消息ID（用于ACK确认和去重）
     */
    void pushWithRetry(String receiverId, Object messageData, String messageType, String messageId);
    
    /**
     * 尝试推送消息，如果失败则加入重试队列（兼容旧接口，messageId 为 null）
     * 
     * @param receiverId 接收方用户ID
     * @param messageData 消息数据
     * @param messageType 消息类型（ORDER_CHANGE, COMMODITY_CHANGE, MESSAGE_NEW, UNREAD_COUNT_UPDATE等）
     */
    default void pushWithRetry(String receiverId, Object messageData, String messageType) {
        pushWithRetry(receiverId, messageData, messageType, null);
    }
    
    /**
     * 处理重试队列中的消息
     * 定时任务调用，检查并重试推送失败的消息
     */
    void retryFailedMessages();
    
    /**
     * 处理ACK确认
     * 收到前端ACK后，从重试队列中移除对应的消息
     * 
     * @param userId 用户ID
     * @param messageId 消息ID
     * @param messageType 消息类型
     */
    void handleAck(String userId, String messageId, String messageType);
}

