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
     */
    void pushWithRetry(String receiverId, Object messageData, String messageType);
    
    /**
     * 处理重试队列中的消息
     * 定时任务调用，检查并重试推送失败的消息
     */
    void retryFailedMessages();
}

